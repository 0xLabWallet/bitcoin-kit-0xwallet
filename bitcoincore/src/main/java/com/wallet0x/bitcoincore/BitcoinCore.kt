package com.wallet0x.bitcoincore

import com.wallet0x.bitcoincore.blocks.IPeerSyncListener
import com.wallet0x.bitcoincore.core.DataProvider
import com.wallet0x.bitcoincore.core.IConnectionManager
import com.wallet0x.bitcoincore.core.IInitialDownload
import com.wallet0x.bitcoincore.core.IKitStateListener
import com.wallet0x.bitcoincore.core.IPluginData
import com.wallet0x.bitcoincore.core.IPublicKeyManager
import com.wallet0x.bitcoincore.core.IStorage
import com.wallet0x.bitcoincore.core.PluginManager
import com.wallet0x.bitcoincore.core.description
import com.wallet0x.bitcoincore.core.scriptType
import com.wallet0x.bitcoincore.extensions.toHexString
import com.wallet0x.bitcoincore.managers.IRestoreKeyConverter
import com.wallet0x.bitcoincore.managers.IUnspentOutputSelector
import com.wallet0x.bitcoincore.managers.RestoreKeyConverterChain
import com.wallet0x.bitcoincore.managers.SyncManager
import com.wallet0x.bitcoincore.managers.UnspentOutputSelectorChain
import com.wallet0x.bitcoincore.models.BalanceInfo
import com.wallet0x.bitcoincore.models.BitcoinPaymentData
import com.wallet0x.bitcoincore.models.BlockInfo
import com.wallet0x.bitcoincore.models.PublicKey
import com.wallet0x.bitcoincore.models.TransactionDataSortType
import com.wallet0x.bitcoincore.models.TransactionFilterType
import com.wallet0x.bitcoincore.models.TransactionInfo
import com.wallet0x.bitcoincore.models.UsedAddress
import com.wallet0x.bitcoincore.network.messages.IMessageParser
import com.wallet0x.bitcoincore.network.messages.IMessageSerializer
import com.wallet0x.bitcoincore.network.messages.NetworkMessageParser
import com.wallet0x.bitcoincore.network.messages.NetworkMessageSerializer
import com.wallet0x.bitcoincore.network.peer.IInventoryItemsHandler
import com.wallet0x.bitcoincore.network.peer.IPeerTaskHandler
import com.wallet0x.bitcoincore.network.peer.InventoryItemsHandlerChain
import com.wallet0x.bitcoincore.network.peer.PeerGroup
import com.wallet0x.bitcoincore.network.peer.PeerManager
import com.wallet0x.bitcoincore.network.peer.PeerTaskHandlerChain
import com.wallet0x.bitcoincore.storage.FullTransaction
import com.wallet0x.bitcoincore.storage.UnspentOutput
import com.wallet0x.bitcoincore.transactions.TransactionCreator
import com.wallet0x.bitcoincore.transactions.TransactionFeeCalculator
import com.wallet0x.bitcoincore.transactions.TransactionSyncer
import com.wallet0x.bitcoincore.transactions.scripts.ScriptType
import com.wallet0x.bitcoincore.utils.AddressConverterChain
import com.wallet0x.bitcoincore.utils.DirectExecutor
import com.wallet0x.bitcoincore.utils.IAddressConverter
import com.wallet0x.bitcoincore.utils.PaymentAddressParser
import com.wallet0x.hdwalletkit.HDWallet.Purpose
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.Executor
import kotlin.math.roundToInt

class BitcoinCore(
    private val storage: IStorage,
    private val dataProvider: DataProvider,
    private val publicKeyManager: IPublicKeyManager,
    private val addressConverter: AddressConverterChain,
    private val restoreKeyConverterChain: RestoreKeyConverterChain,
    private val transactionCreator: TransactionCreator?,
    private val transactionFeeCalculator: TransactionFeeCalculator?,
    private val paymentAddressParser: PaymentAddressParser,
    private val syncManager: SyncManager,
    private val purpose: Purpose,
    private var peerManager: PeerManager,
    private val dustCalculator: DustCalculator?,
    private val pluginManager: PluginManager,
    private val connectionManager: IConnectionManager
) : IKitStateListener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) = Unit
        fun onTransactionsDelete(hashes: List<String>) = Unit
        fun onBalanceUpdate(balance: BalanceInfo) = Unit
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo) = Unit
        fun onKitStateUpdate(state: KitState) = Unit
    }

    // START: Extending
    lateinit var peerGroup: PeerGroup
    lateinit var transactionSyncer: TransactionSyncer
    lateinit var networkMessageParser: NetworkMessageParser
    lateinit var networkMessageSerializer: NetworkMessageSerializer
    lateinit var initialDownload: IInitialDownload
    lateinit var unspentOutputSelector: UnspentOutputSelectorChain
    lateinit var watchedTransactionManager: WatchedTransactionManager

    val inventoryItemsHandlerChain = InventoryItemsHandlerChain()
    val peerTaskHandlerChain = PeerTaskHandlerChain()

    fun addPeerSyncListener(peerSyncListener: IPeerSyncListener): BitcoinCore {
        initialDownload.addPeerSyncListener(peerSyncListener)
        return this
    }

    fun addRestoreKeyConverter(keyConverter: IRestoreKeyConverter) {
        restoreKeyConverterChain.add(keyConverter)
    }

    fun addMessageParser(messageParser: IMessageParser): BitcoinCore {
        networkMessageParser.add(messageParser)
        return this
    }

    fun addMessageSerializer(messageSerializer: IMessageSerializer): BitcoinCore {
        networkMessageSerializer.add(messageSerializer)
        return this
    }

    fun addInventoryItemsHandler(handler: IInventoryItemsHandler) {
        inventoryItemsHandlerChain.addHandler(handler)
    }

    fun addPeerTaskHandler(handler: IPeerTaskHandler) {
        peerTaskHandlerChain.addHandler(handler)
    }

    fun addPeerGroupListener(listener: PeerGroup.Listener) {
        peerGroup.addPeerGroupListener(listener)
    }

    fun prependUnspentOutputSelector(selector: IUnspentOutputSelector) {
        unspentOutputSelector.prependSelector(selector)
    }

    fun prependAddressConverter(converter: IAddressConverter) {
        addressConverter.prependConverter(converter)
    }

    // END: Extending

    var listenerExecutor: Executor = DirectExecutor()

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val lastBlockInfo get() = dataProvider.lastBlockInfo
    val syncState get() = syncManager.syncState

    var listener: Listener? = null

    val watchAccount: Boolean
        get() = transactionCreator == null

    //
    // API methods
    //
    fun start() {
        syncManager.start()
    }

    fun stop() {
        dataProvider.clear()
        syncManager.stop()
    }

    fun refresh() {
        start()
    }

    fun onEnterForeground() {
        connectionManager.onEnterForeground()
    }

    fun onEnterBackground() {
        connectionManager.onEnterBackground()
    }

    fun transactions(fromUid: String? = null, type: TransactionFilterType? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return dataProvider.transactions(fromUid, type, limit)
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int, pluginData: Map<Byte, IPluginData>): Long {
        return transactionFeeCalculator?.fee(value, feeRate, senderPay, address, pluginData) ?: throw CoreError.ReadOnlyCore
    }

    fun fee(
        unspentOutputs: List<UnspentOutput>,
        address: String? = null,
        feeRate: Int,
        pluginData: Map<Byte, IPluginData>
    ): Long {
        return transactionFeeCalculator?.fee(
            unspentOutputs,
            feeRate,
            address,
            pluginData
        ) ?: throw CoreError.ReadOnlyCore
    }

    fun getSpendableUtxo() = dataProvider.getSpendableUtxo()

    fun send(
        address: String,
        unspentOutputs: List<UnspentOutput>,
        feeRate: Int,
        sortType: TransactionDataSortType,
        pluginData: Map<Byte, IPluginData>
    ): FullTransaction {
        return transactionCreator?.create(address, unspentOutputs, feeRate, sortType, pluginData) ?: throw CoreError.ReadOnlyCore
    }

    fun send(
        address: String,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        pluginData: Map<Byte, IPluginData>
    ): FullTransaction {
        return transactionCreator?.create(address, value, feeRate, senderPay, sortType, pluginData) ?: throw CoreError.ReadOnlyCore
    }

    fun send(
        hash: ByteArray,
        scriptType: ScriptType,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType
    ): FullTransaction {
        val address = addressConverter.convert(hash, scriptType)
        return transactionCreator?.create(address.stringValue, value, feeRate, senderPay, sortType, mapOf()) ?: throw CoreError.ReadOnlyCore
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, sortType: TransactionDataSortType): FullTransaction {
        return transactionCreator?.create(unspentOutput, address, feeRate, sortType) ?: throw CoreError.ReadOnlyCore
    }

    fun receiveAddress(): String {
        return addressConverter.convert(publicKeyManager.receivePublicKey(), purpose.scriptType).stringValue
    }

    fun usedAddresses(): List<UsedAddress> {
        return publicKeyManager.usedExternalPublicKeys().map {
            UsedAddress(
                index = it.index,
                address = addressConverter.convert(it, purpose.scriptType).stringValue
            )
        }.sortedBy { it.index }
    }

    fun receivePublicKey(): PublicKey {
        return publicKeyManager.receivePublicKey()
    }

    fun changePublicKey(): PublicKey {
        return publicKeyManager.changePublicKey()
    }

    fun getPublicKeyByPath(path: String): PublicKey {
        return publicKeyManager.getPublicKeyByPath(path)
    }

    fun validateAddress(address: String, pluginData: Map<Byte, IPluginData> = mapOf()) {
        pluginManager.validateAddress(addressConverter.convert(address), pluginData)
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return paymentAddressParser.parse(paymentAddress)
    }

    fun showDebugInfo() {
        publicKeyManager.fillGap()
        storage.getPublicKeys().forEach { pubKey ->
            try {
//                    val scriptType = if (network is MainNetBitcoinCash || network is TestNetBitcoinCash)
//                        ScriptType.P2PKH else
//                        ScriptType.P2WPKH

                val legacy = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).stringValue
//                    val wpkh = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
//                    val bechAddress = try {
//                        addressConverter.convert(OpCodes.push(0) + OpCodes.push(pubKey.publicKeyHash), scriptType).string
//                    } catch (e: Exception) {
//                        ""
//                    }
                println("${pubKey.index} --- extrnl: ${pubKey.external} --- hash: ${pubKey.publicKeyHash.toHexString()} ---- legacy: $legacy")
//                    println("legacy: $legacy --- bech32: $bechAddress --- SH(WPKH): $wpkh")
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    fun statusInfo(): Map<String, Any> {
        val statusInfo = LinkedHashMap<String, Any>()

        statusInfo["Synced Until"] = lastBlockInfo?.timestamp?.let { Date(it * 1000) } ?: "N/A"
        statusInfo["Syncing Peer"] = initialDownload.syncPeer?.host ?: "N/A"
        statusInfo["Derivation"] = purpose.description
        statusInfo["Sync State"] = syncState.toString()
        statusInfo["Last Block Height"] = lastBlockInfo?.height ?: "N/A"

        val peers = LinkedHashMap<String, Any>()
        peerManager.connected().forEachIndexed { index, peer ->

            val peerStatus = LinkedHashMap<String, Any>()
            peerStatus["Status"] = if (peer.synced) "Synced" else "Not Synced"
            peerStatus["Host"] = peer.host
            peerStatus["Best Block"] = peer.announcedLastBlockHeight
            peerStatus["User Agent"] = peer.subVersion

            peer.tasks.let { peerTasks ->
                if (peerTasks.isEmpty()) {
                    peerStatus["tasks"] = "no tasks"
                } else {
                    val tasks = LinkedHashMap<String, Any>()
                    peerTasks.forEach { task ->
                        tasks[task.javaClass.simpleName] = "[${task.state}]"
                    }
                    peerStatus["tasks"] = tasks
                }
            }

            peers["Peer ${index + 1}"] = peerStatus
        }

        statusInfo.putAll(peers)

        return statusInfo
    }

    //
    // DataProvider Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        listenerExecutor.execute {
            listener?.onTransactionsUpdate(inserted, updated)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        listenerExecutor.execute {
            listener?.onTransactionsDelete(hashes)
        }
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        listenerExecutor.execute {
            listener?.onBalanceUpdate(balance)
        }
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listenerExecutor.execute {
            listener?.onLastBlockInfoUpdate(blockInfo)
        }
    }

    //
    // IKitStateManagerListener implementations
    //
    override fun onKitStateUpdate(state: KitState) {
        listenerExecutor.execute {
            listener?.onKitStateUpdate(state)
        }
    }

    fun watchTransaction(filter: TransactionFilter, listener: WatchedTransactionManager.Listener) {
        watchedTransactionManager.add(filter, listener)
    }

    fun maximumSpendableValue(address: String?, feeRate: Int, pluginData: Map<Byte, IPluginData>): Long {
        return transactionFeeCalculator?.let { transactionFeeCalculator ->
            balance.spendable - transactionFeeCalculator.fee(balance.spendable, feeRate, false, address, pluginData)
        } ?: throw CoreError.ReadOnlyCore
    }

    fun minimumSpendableValue(address: String?): Int {
        // by default script type is P2PKH, since it is most used
        val scriptType = when {
            address != null -> addressConverter.convert(address).scriptType
            else -> ScriptType.P2PKH
        }

        return dustCalculator?.dust(scriptType) ?: throw CoreError.ReadOnlyCore
    }

    fun getRawTransaction(transactionHash: String): String? {
        return dataProvider.getRawTransaction(transactionHash)
    }

    fun getTransaction(hash: String): TransactionInfo? {
        return dataProvider.getTransaction(hash)
    }

    sealed class KitState {
        object Synced : KitState()
        class NotSynced(val exception: Throwable) : KitState()
        class Syncing(val progress: Double) : KitState()
        class ApiSyncing(val transactions: Int) : KitState()

        override fun equals(other: Any?) = when {
            this is Synced && other is Synced -> true
            this is NotSynced && other is NotSynced -> exception == other.exception
            this is Syncing && other is Syncing -> this.progress == other.progress
            this is ApiSyncing && other is ApiSyncing -> this.transactions == other.transactions
            else -> false
        }

        override fun toString() = when (this) {
            is Synced -> "Synced"
            is NotSynced -> "NotSynced-${this.exception.javaClass.simpleName}"
            is Syncing -> "Syncing-${(this.progress * 100).roundToInt() / 100.0}"
            is ApiSyncing -> "ApiSyncing-$transactions"
        }

        override fun hashCode(): Int {
            var result = javaClass.hashCode()
            if (this is Syncing) {
                result = 31 * result + progress.hashCode()
            }
            if (this is NotSynced) {
                result = 31 * result + exception.hashCode()
            }
            if (this is ApiSyncing) {
                result = 31 * result + transactions.hashCode()
            }
            return result
        }
    }

    sealed class SyncMode {
        class Full : SyncMode()
        class Api : SyncMode()
        class Blockchair(val key: String) : SyncMode()
    }

    sealed class StateError : Exception() {
        class NotStarted : StateError()
        class NoInternet : StateError()
    }

    sealed class CoreError : Exception() {
        object ReadOnlyCore : CoreError()
    }

}
