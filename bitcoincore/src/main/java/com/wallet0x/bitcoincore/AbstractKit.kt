package com.wallet0x.bitcoincore

import com.wallet0x.bitcoincore.core.IPluginData
import com.wallet0x.bitcoincore.models.BitcoinPaymentData
import com.wallet0x.bitcoincore.models.PublicKey
import com.wallet0x.bitcoincore.models.TransactionDataSortType
import com.wallet0x.bitcoincore.models.TransactionFilterType
import com.wallet0x.bitcoincore.models.TransactionInfo
import com.wallet0x.bitcoincore.models.UsedAddress
import com.wallet0x.bitcoincore.network.Network
import com.wallet0x.bitcoincore.storage.FullTransaction
import com.wallet0x.bitcoincore.storage.UnspentOutput
import com.wallet0x.bitcoincore.transactions.scripts.ScriptType
import io.reactivex.Single

abstract class AbstractKit {

    protected abstract var bitcoinCore: BitcoinCore
    protected abstract var network: Network

    val balance
        get() = bitcoinCore.balance

    val lastBlockInfo
        get() = bitcoinCore.lastBlockInfo

    val networkName: String
        get() = network.javaClass.simpleName

    val syncState get() = bitcoinCore.syncState

    val watchAccount: Boolean
        get() = bitcoinCore.watchAccount

    fun start() {
        bitcoinCore.start()
    }

    fun stop() {
        bitcoinCore.stop()
    }

    fun refresh() {
        bitcoinCore.refresh()
    }

    fun onEnterForeground() {
        bitcoinCore.onEnterForeground()
    }

    fun onEnterBackground() {
        bitcoinCore.onEnterBackground()
    }

    fun getSpendableUtxo() = bitcoinCore.getSpendableUtxo()

    fun transactions(fromUid: String? = null, type: TransactionFilterType? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return bitcoinCore.transactions(fromUid, type, limit)
    }

    fun getTransaction(hash: String): TransactionInfo? {
        return bitcoinCore.getTransaction(hash)
    }

    fun fee(
        unspentOutputs: List<UnspentOutput>,
        address: String? = null,
        feeRate: Int,
        pluginData: Map<Byte, IPluginData>
    ): Long {
        return bitcoinCore.fee(unspentOutputs, address, feeRate, pluginData)
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int, pluginData: Map<Byte, IPluginData> = mapOf()): Long {
        return bitcoinCore.fee(value, address, senderPay, feeRate, pluginData)
    }

    fun send(
        address: String,
        unspentOutputs: List<UnspentOutput>,
        feeRate: Int,
        sortType: TransactionDataSortType,
        pluginData: Map<Byte, IPluginData>
    ): FullTransaction {
        return bitcoinCore.send(address, unspentOutputs, feeRate, sortType, pluginData)
    }

    fun send(
        address: String,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        pluginData: Map<Byte, IPluginData> = mapOf()
    ): FullTransaction {
        return bitcoinCore.send(address, value, senderPay, feeRate, sortType, pluginData)
    }

    fun send(
        hash: ByteArray,
        scriptType: ScriptType,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType
    ): FullTransaction {
        return bitcoinCore.send(hash, scriptType, value, senderPay, feeRate, sortType)
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, sortType: TransactionDataSortType): FullTransaction {
        return bitcoinCore.redeem(unspentOutput, address, feeRate, sortType)
    }

    fun receiveAddress(): String {
        return bitcoinCore.receiveAddress()
    }

    fun usedAddresses(): List<UsedAddress> {
        return bitcoinCore.usedAddresses()
    }

    fun receivePublicKey(): PublicKey {
        return bitcoinCore.receivePublicKey()
    }

    fun changePublicKey(): PublicKey {
        return bitcoinCore.changePublicKey()
    }

    fun validateAddress(address: String, pluginData: Map<Byte, IPluginData>) {
        bitcoinCore.validateAddress(address, pluginData)
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return bitcoinCore.parsePaymentAddress(paymentAddress)
    }

    fun showDebugInfo() {
        bitcoinCore.showDebugInfo()
    }

    fun statusInfo(): Map<String, Any> {
        return bitcoinCore.statusInfo()
    }

    fun getPublicKeyByPath(path: String): PublicKey {
        return bitcoinCore.getPublicKeyByPath(path)
    }

    fun watchTransaction(filter: TransactionFilter, listener: WatchedTransactionManager.Listener) {
        bitcoinCore.watchTransaction(filter, listener)
    }

    fun maximumSpendableValue(address: String?, feeRate: Int, pluginData: Map<Byte, IPluginData>): Long {
        return bitcoinCore.maximumSpendableValue(address, feeRate, pluginData)
    }

    fun minimumSpendableValue(address: String?): Int {
        return bitcoinCore.minimumSpendableValue(address)
    }

    fun getRawTransaction(transactionHash: String): String? {
        return bitcoinCore.getRawTransaction(transactionHash)
    }
}
