package com.wallet0x.bitcoincore

import android.content.Context
import com.wallet0x.bitcoincore.apisync.blockchair.BlockchairApi
import com.wallet0x.bitcoincore.apisync.blockchair.BlockchairApiSyncer
import com.wallet0x.bitcoincore.apisync.blockchair.BlockchairLastBlockProvider
import com.wallet0x.bitcoincore.apisync.blockchair.BlockchairTransactionProvider
import com.wallet0x.bitcoincore.apisync.legacy.ApiSyncer
import com.wallet0x.bitcoincore.apisync.legacy.BlockHashDiscoveryBatch
import com.wallet0x.bitcoincore.apisync.legacy.BlockHashScanHelper
import com.wallet0x.bitcoincore.apisync.legacy.BlockHashScanner
import com.wallet0x.bitcoincore.apisync.legacy.IMultiAccountPublicKeyFetcher
import com.wallet0x.bitcoincore.apisync.legacy.IPublicKeyFetcher
import com.wallet0x.bitcoincore.apisync.legacy.MultiAccountPublicKeyFetcher
import com.wallet0x.bitcoincore.apisync.legacy.PublicKeyFetcher
import com.wallet0x.bitcoincore.apisync.legacy.WatchAddressBlockHashScanHelper
import com.wallet0x.bitcoincore.apisync.legacy.WatchPublicKeyFetcher
import com.wallet0x.bitcoincore.blocks.BlockDownload
import com.wallet0x.bitcoincore.blocks.BlockSyncer
import com.wallet0x.bitcoincore.blocks.Blockchain
import com.wallet0x.bitcoincore.blocks.BloomFilterLoader
import com.wallet0x.bitcoincore.blocks.InitialBlockDownload
import com.wallet0x.bitcoincore.blocks.MerkleBlockExtractor
import com.wallet0x.bitcoincore.blocks.validators.IBlockValidator
import com.wallet0x.bitcoincore.core.AccountWallet
import com.wallet0x.bitcoincore.core.BaseTransactionInfoConverter
import com.wallet0x.bitcoincore.core.DataProvider
import com.wallet0x.bitcoincore.core.DoubleSha256Hasher
import com.wallet0x.bitcoincore.core.IApiSyncer
import com.wallet0x.bitcoincore.core.IApiTransactionProvider
import com.wallet0x.bitcoincore.core.IHasher
import com.wallet0x.bitcoincore.core.IInitialDownload
import com.wallet0x.bitcoincore.core.IPlugin
import com.wallet0x.bitcoincore.core.IPrivateWallet
import com.wallet0x.bitcoincore.core.IPublicKeyManager
import com.wallet0x.bitcoincore.core.IStorage
import com.wallet0x.bitcoincore.core.ITransactionInfoConverter
import com.wallet0x.bitcoincore.core.PluginManager
import com.wallet0x.bitcoincore.core.TransactionDataSorterFactory
import com.wallet0x.bitcoincore.core.TransactionInfoConverter
import com.wallet0x.bitcoincore.core.Wallet
import com.wallet0x.bitcoincore.core.WatchAccountWallet
import com.wallet0x.bitcoincore.core.scriptType
import com.wallet0x.bitcoincore.managers.AccountPublicKeyManager
import com.wallet0x.bitcoincore.managers.ApiSyncStateManager
import com.wallet0x.bitcoincore.managers.BloomFilterManager
import com.wallet0x.bitcoincore.managers.ConnectionManager
import com.wallet0x.bitcoincore.managers.IBloomFilterProvider
import com.wallet0x.bitcoincore.managers.IrregularOutputFinder
import com.wallet0x.bitcoincore.managers.PendingOutpointsProvider
import com.wallet0x.bitcoincore.managers.PublicKeyManager
import com.wallet0x.bitcoincore.managers.RestoreKeyConverterChain
import com.wallet0x.bitcoincore.managers.SyncManager
import com.wallet0x.bitcoincore.managers.UnspentOutputProvider
import com.wallet0x.bitcoincore.managers.UnspentOutputSelector
import com.wallet0x.bitcoincore.managers.UnspentOutputSelectorChain
import com.wallet0x.bitcoincore.managers.UnspentOutputSelectorSingleNoChange
import com.wallet0x.bitcoincore.models.Checkpoint
import com.wallet0x.bitcoincore.models.WatchAddressPublicKey
import com.wallet0x.bitcoincore.network.Network
import com.wallet0x.bitcoincore.network.messages.AddrMessageParser
import com.wallet0x.bitcoincore.network.messages.FilterLoadMessageSerializer
import com.wallet0x.bitcoincore.network.messages.GetBlocksMessageSerializer
import com.wallet0x.bitcoincore.network.messages.GetDataMessageParser
import com.wallet0x.bitcoincore.network.messages.GetDataMessageSerializer
import com.wallet0x.bitcoincore.network.messages.InvMessageParser
import com.wallet0x.bitcoincore.network.messages.InvMessageSerializer
import com.wallet0x.bitcoincore.network.messages.MempoolMessageSerializer
import com.wallet0x.bitcoincore.network.messages.MerkleBlockMessageParser
import com.wallet0x.bitcoincore.network.messages.NetworkMessageParser
import com.wallet0x.bitcoincore.network.messages.NetworkMessageSerializer
import com.wallet0x.bitcoincore.network.messages.PingMessageParser
import com.wallet0x.bitcoincore.network.messages.PingMessageSerializer
import com.wallet0x.bitcoincore.network.messages.PongMessageParser
import com.wallet0x.bitcoincore.network.messages.PongMessageSerializer
import com.wallet0x.bitcoincore.network.messages.RejectMessageParser
import com.wallet0x.bitcoincore.network.messages.TransactionMessageParser
import com.wallet0x.bitcoincore.network.messages.TransactionMessageSerializer
import com.wallet0x.bitcoincore.network.messages.VerAckMessageParser
import com.wallet0x.bitcoincore.network.messages.VerAckMessageSerializer
import com.wallet0x.bitcoincore.network.messages.VersionMessageParser
import com.wallet0x.bitcoincore.network.messages.VersionMessageSerializer
import com.wallet0x.bitcoincore.network.peer.MempoolTransactions
import com.wallet0x.bitcoincore.network.peer.PeerAddressManager
import com.wallet0x.bitcoincore.network.peer.PeerGroup
import com.wallet0x.bitcoincore.network.peer.PeerManager
import com.wallet0x.bitcoincore.serializers.BlockHeaderParser
import com.wallet0x.bitcoincore.transactions.BlockTransactionProcessor
import com.wallet0x.bitcoincore.transactions.PendingTransactionProcessor
import com.wallet0x.bitcoincore.transactions.SendTransactionsOnPeersSynced
import com.wallet0x.bitcoincore.transactions.TransactionConflictsResolver
import com.wallet0x.bitcoincore.transactions.TransactionCreator
import com.wallet0x.bitcoincore.transactions.TransactionFeeCalculator
import com.wallet0x.bitcoincore.transactions.TransactionInvalidator
import com.wallet0x.bitcoincore.transactions.TransactionSendTimer
import com.wallet0x.bitcoincore.transactions.TransactionSender
import com.wallet0x.bitcoincore.transactions.TransactionSizeCalculator
import com.wallet0x.bitcoincore.transactions.TransactionSyncer
import com.wallet0x.bitcoincore.transactions.builder.EcdsaInputSigner
import com.wallet0x.bitcoincore.transactions.builder.InputSetter
import com.wallet0x.bitcoincore.transactions.builder.LockTimeSetter
import com.wallet0x.bitcoincore.transactions.builder.OutputSetter
import com.wallet0x.bitcoincore.transactions.builder.RecipientSetter
import com.wallet0x.bitcoincore.transactions.builder.SchnorrInputSigner
import com.wallet0x.bitcoincore.transactions.builder.TransactionBuilder
import com.wallet0x.bitcoincore.transactions.builder.TransactionSigner
import com.wallet0x.bitcoincore.transactions.extractors.MyOutputsCache
import com.wallet0x.bitcoincore.transactions.extractors.TransactionExtractor
import com.wallet0x.bitcoincore.transactions.extractors.TransactionMetadataExtractor
import com.wallet0x.bitcoincore.transactions.extractors.TransactionOutputProvider
import com.wallet0x.bitcoincore.transactions.scripts.ScriptType
import com.wallet0x.bitcoincore.utils.AddressConverterChain
import com.wallet0x.bitcoincore.utils.Base58AddressConverter
import com.wallet0x.bitcoincore.utils.PaymentAddressParser
import com.wallet0x.hdwalletkit.HDExtendedKey
import com.wallet0x.hdwalletkit.HDWallet
import com.wallet0x.hdwalletkit.HDWalletAccount
import com.wallet0x.hdwalletkit.HDWalletAccountWatch

class BitcoinCoreBuilder {

    val addressConverter = AddressConverterChain()

    // required parameters
    private var context: Context? = null
    private var extendedKey: HDExtendedKey? = null
    private var watchAddressPublicKey: WatchAddressPublicKey? = null
    private var purpose: HDWallet.Purpose? = null
    private var network: Network? = null
    private var paymentAddressParser: PaymentAddressParser? = null
    private var storage: IStorage? = null
    private var apiTransactionProvider: IApiTransactionProvider? = null
    private var blockHeaderHasher: IHasher? = null
    private var transactionInfoConverter: ITransactionInfoConverter? = null
    private var blockValidator: IBlockValidator? = null
    private var checkpoint: Checkpoint? = null
    private var apiSyncStateManager: ApiSyncStateManager? = null

    // parameters with default values
    private var confirmationsThreshold = 6
    private var syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
    private var peerSize = 10
    private val plugins = mutableListOf<IPlugin>()
    private var handleAddrMessage = true

    fun setContext(context: Context): BitcoinCoreBuilder {
        this.context = context
        return this
    }

    fun setExtendedKey(extendedKey: HDExtendedKey?): BitcoinCoreBuilder {
        this.extendedKey = extendedKey
        return this
    }

    fun setWatchAddressPublicKey(publicKey: WatchAddressPublicKey?): BitcoinCoreBuilder {
        this.watchAddressPublicKey = publicKey
        return this
    }

    fun setPurpose(purpose: HDWallet.Purpose): BitcoinCoreBuilder {
        this.purpose = purpose
        return this
    }

    fun setNetwork(network: Network): BitcoinCoreBuilder {
        this.network = network
        return this
    }

    fun setPaymentAddressParser(paymentAddressParser: PaymentAddressParser): BitcoinCoreBuilder {
        this.paymentAddressParser = paymentAddressParser
        return this
    }

    fun setConfirmationThreshold(confirmationsThreshold: Int): BitcoinCoreBuilder {
        this.confirmationsThreshold = confirmationsThreshold
        return this
    }

    fun setSyncMode(syncMode: BitcoinCore.SyncMode): BitcoinCoreBuilder {
        this.syncMode = syncMode
        return this
    }

    fun setPeerSize(peerSize: Int): BitcoinCoreBuilder {
        if (peerSize < TransactionSender.minConnectedPeerSize) {
            throw Error("Peer size cannot be less than ${TransactionSender.minConnectedPeerSize}")
        }

        this.peerSize = peerSize
        return this
    }

    fun setStorage(storage: IStorage): BitcoinCoreBuilder {
        this.storage = storage
        return this
    }

    fun setBlockHeaderHasher(blockHeaderHasher: IHasher): BitcoinCoreBuilder {
        this.blockHeaderHasher = blockHeaderHasher
        return this
    }

    fun setApiTransactionProvider(apiTransactionProvider: IApiTransactionProvider?): BitcoinCoreBuilder {
        this.apiTransactionProvider = apiTransactionProvider
        return this
    }

    fun setTransactionInfoConverter(transactionInfoConverter: ITransactionInfoConverter): BitcoinCoreBuilder {
        this.transactionInfoConverter = transactionInfoConverter
        return this
    }

    fun setBlockValidator(blockValidator: IBlockValidator): BitcoinCoreBuilder {
        this.blockValidator = blockValidator
        return this
    }

    fun setHandleAddrMessage(handle: Boolean): BitcoinCoreBuilder {
        handleAddrMessage = handle
        return this
    }

    fun addPlugin(plugin: IPlugin): BitcoinCoreBuilder {
        plugins.add(plugin)
        return this
    }

    fun setCheckpoint(checkpoint: Checkpoint): BitcoinCoreBuilder {
        this.checkpoint = checkpoint
        return this
    }

    fun setApiSyncStateManager(apiSyncStateManager: ApiSyncStateManager): BitcoinCoreBuilder {
        this.apiSyncStateManager = apiSyncStateManager
        return this
    }

    fun build(): BitcoinCore {
        val context = checkNotNull(this.context)
        val extendedKey = this.extendedKey
        val watchAddressPublicKey = this.watchAddressPublicKey
        val purpose = checkNotNull(this.purpose)
        val network = checkNotNull(this.network)
        val paymentAddressParser = checkNotNull(this.paymentAddressParser)
        val storage = checkNotNull(this.storage)
        val apiTransactionProvider = checkNotNull(this.apiTransactionProvider)
        val checkpoint = checkNotNull(this.checkpoint)
        val apiSyncStateManager = checkNotNull(this.apiSyncStateManager)
        val blockHeaderHasher = this.blockHeaderHasher ?: DoubleSha256Hasher()
        val transactionInfoConverter = this.transactionInfoConverter ?: TransactionInfoConverter()

        val restoreKeyConverterChain = RestoreKeyConverterChain()

        val pluginManager = PluginManager()
        plugins.forEach { pluginManager.addPlugin(it) }

        transactionInfoConverter.baseConverter = BaseTransactionInfoConverter(pluginManager)

        val unspentOutputProvider = UnspentOutputProvider(storage, confirmationsThreshold, pluginManager)

        val dataProvider = DataProvider(storage, unspentOutputProvider, transactionInfoConverter)

        val connectionManager = ConnectionManager(context)

        var privateWallet: IPrivateWallet? = null
        val publicKeyFetcher: IPublicKeyFetcher
        var multiAccountPublicKeyFetcher: IMultiAccountPublicKeyFetcher? = null
        val publicKeyManager: IPublicKeyManager
        val bloomFilterProvider: IBloomFilterProvider
        val gapLimit = 20

        if (watchAddressPublicKey != null) {
            storage.savePublicKeys(listOf(watchAddressPublicKey))

            WatchAddressPublicKeyManager(watchAddressPublicKey, restoreKeyConverterChain).let {
                publicKeyFetcher = it
                publicKeyManager = it
                bloomFilterProvider = it
            }
        } else if (extendedKey != null) {
            if (!extendedKey.isPublic) {
                when (extendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master -> {
                        val wallet = Wallet(HDWallet(extendedKey.key, network.coinType, purpose), gapLimit)
                        privateWallet = wallet
                        val fetcher = MultiAccountPublicKeyFetcher(wallet)
                        publicKeyFetcher = fetcher
                        multiAccountPublicKeyFetcher = fetcher
                        PublicKeyManager.create(storage, wallet, restoreKeyConverterChain).apply {
                            publicKeyManager = this
                            bloomFilterProvider = this
                        }
                    }

                    HDExtendedKey.DerivedType.Account -> {
                        val wallet = AccountWallet(HDWalletAccount(extendedKey.key), gapLimit)
                        privateWallet = wallet
                        val fetcher = PublicKeyFetcher(wallet)
                        publicKeyFetcher = fetcher
                        AccountPublicKeyManager.create(storage, wallet, restoreKeyConverterChain).apply {
                            publicKeyManager = this
                            bloomFilterProvider = this
                        }

                    }

                    HDExtendedKey.DerivedType.Bip32 -> {
                        throw IllegalStateException("Custom Bip32 Extended Keys are not supported")
                    }
                }
            } else {
                when (extendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Account -> {
                        val wallet = WatchAccountWallet(HDWalletAccountWatch(extendedKey.key), gapLimit)
                        val fetcher = WatchPublicKeyFetcher(wallet)
                        publicKeyFetcher = fetcher
                        AccountPublicKeyManager.create(storage, wallet, restoreKeyConverterChain).apply {
                            publicKeyManager = this
                            bloomFilterProvider = this
                        }

                    }

                    HDExtendedKey.DerivedType.Bip32, HDExtendedKey.DerivedType.Master -> {
                        throw IllegalStateException("Only Account Extended Public Keys are supported")
                    }
                }
            }
        } else {
            throw IllegalStateException("Both extendedKey and watchAddressPublicKey are NULL!")
        }

        val pendingOutpointsProvider = PendingOutpointsProvider(storage)

        val additionalScriptTypes = if (watchAddressPublicKey != null) listOf(ScriptType.P2PKH) else emptyList()
        val irregularOutputFinder = IrregularOutputFinder(storage, additionalScriptTypes)
        val metadataExtractor = TransactionMetadataExtractor(
            MyOutputsCache.create(storage),
            TransactionOutputProvider(storage)
        )
        val transactionExtractor = TransactionExtractor(addressConverter, storage, pluginManager, metadataExtractor)

        val conflictsResolver = TransactionConflictsResolver(storage)
        val pendingTransactionProcessor = PendingTransactionProcessor(
            storage,
            transactionExtractor,
            publicKeyManager,
            irregularOutputFinder,
            dataProvider,
            conflictsResolver
        )
        val invalidator = TransactionInvalidator(storage, transactionInfoConverter, dataProvider)
        val blockTransactionProcessor = BlockTransactionProcessor(
            storage,
            transactionExtractor,
            publicKeyManager,
            irregularOutputFinder,
            dataProvider,
            conflictsResolver,
            invalidator
        )

        val peerHostManager = PeerAddressManager(network, storage)
        val bloomFilterManager = BloomFilterManager()

        val peerManager = PeerManager()

        val networkMessageParser = NetworkMessageParser(network.magic)
        val networkMessageSerializer = NetworkMessageSerializer(network.magic)

        val blockchain = Blockchain(storage, blockValidator, dataProvider)
        val blockSyncer = BlockSyncer(storage, blockchain, blockTransactionProcessor, publicKeyManager, checkpoint)


        val peerGroup = PeerGroup(
            peerHostManager,
            network,
            peerManager,
            peerSize,
            networkMessageParser,
            networkMessageSerializer,
            connectionManager,
            blockSyncer.localDownloadedBestBlockHeight,
            handleAddrMessage
        )
        peerHostManager.listener = peerGroup

        val blockHashScanHelper = if (watchAddressPublicKey == null) BlockHashScanHelper() else WatchAddressBlockHashScanHelper()
        val blockHashScanner = BlockHashScanner(restoreKeyConverterChain, apiTransactionProvider, blockHashScanHelper)

        val apiSyncer: IApiSyncer
        val initialDownload: IInitialDownload
        val merkleBlockExtractor = MerkleBlockExtractor(network.maxBlockSize)

        when (val syncMode = syncMode) {
            is BitcoinCore.SyncMode.Blockchair -> {
                val blockchairApi = if (apiTransactionProvider is BlockchairTransactionProvider) {
                    apiTransactionProvider.blockchairApi
                } else {
                    BlockchairApi(syncMode.key, network.blockchairChainId)
                }
                val lastBlockProvider = BlockchairLastBlockProvider(blockchairApi)
                apiSyncer = BlockchairApiSyncer(
                    storage,
                    restoreKeyConverterChain,
                    apiTransactionProvider,
                    lastBlockProvider,
                    publicKeyManager,
                    blockchain,
                    apiSyncStateManager
                )
                initialDownload = BlockDownload(blockSyncer, peerManager, merkleBlockExtractor)
            }

            else -> {
                val blockDiscovery = BlockHashDiscoveryBatch(blockHashScanner, publicKeyFetcher, checkpoint.block.height, gapLimit)
                apiSyncer = ApiSyncer(
                    storage,
                    blockDiscovery,
                    publicKeyManager,
                    multiAccountPublicKeyFetcher,
                    apiSyncStateManager
                )
                initialDownload = InitialBlockDownload(blockSyncer, peerManager, merkleBlockExtractor)
            }
        }

        val syncManager = SyncManager(connectionManager, apiSyncer, peerGroup, storage, syncMode, blockSyncer.localDownloadedBestBlockHeight)
        apiSyncer.listener = syncManager
        connectionManager.listener = syncManager
        blockSyncer.listener = syncManager
        initialDownload.listener = syncManager
        blockHashScanner.listener = syncManager

        val unspentOutputSelector = UnspentOutputSelectorChain()
        val pendingTransactionSyncer = TransactionSyncer(storage, pendingTransactionProcessor, invalidator, publicKeyManager)
        val transactionDataSorterFactory = TransactionDataSorterFactory()

        var dustCalculator: DustCalculator? = null
        var transactionSizeCalculator: TransactionSizeCalculator? = null
        var transactionFeeCalculator: TransactionFeeCalculator? = null
        var transactionSender: TransactionSender? = null
        var transactionCreator: TransactionCreator? = null

        if (privateWallet != null) {
            val ecdsaInputSigner = EcdsaInputSigner(privateWallet, network)
            val schnorrInputSigner = SchnorrInputSigner(privateWallet)
            val transactionSizeCalculatorInstance = TransactionSizeCalculator()
            val dustCalculatorInstance = DustCalculator(network.dustRelayTxFee, transactionSizeCalculatorInstance)
            val recipientSetter = RecipientSetter(addressConverter, pluginManager)
            val outputSetter = OutputSetter(transactionDataSorterFactory)
            val inputSetter = InputSetter(
                unspentOutputSelector,
                publicKeyManager,
                addressConverter,
                purpose.scriptType,
                transactionSizeCalculatorInstance,
                pluginManager,
                dustCalculatorInstance,
                transactionDataSorterFactory
            )
            val lockTimeSetter = LockTimeSetter(storage)
            val signer = TransactionSigner(ecdsaInputSigner, schnorrInputSigner)
            val transactionBuilder = TransactionBuilder(recipientSetter, outputSetter, inputSetter, signer, lockTimeSetter)
            transactionFeeCalculator = TransactionFeeCalculator(
                recipientSetter,
                inputSetter,
                addressConverter,
                publicKeyManager,
                purpose.scriptType,
                transactionSizeCalculatorInstance
            )
            val transactionSendTimer = TransactionSendTimer(60)
            val transactionSenderInstance = TransactionSender(
                pendingTransactionSyncer,
                peerManager,
                initialDownload,
                storage,
                transactionSendTimer
            )

            dustCalculator = dustCalculatorInstance
            transactionSizeCalculator = transactionSizeCalculatorInstance
            transactionSender = transactionSenderInstance

            transactionSendTimer.listener = transactionSender

            transactionCreator = TransactionCreator(transactionBuilder, pendingTransactionProcessor, transactionSenderInstance, bloomFilterManager)
        }

        val bitcoinCore = BitcoinCore(
            storage,
            dataProvider,
            publicKeyManager,
            addressConverter,
            restoreKeyConverterChain,
            transactionCreator,
            transactionFeeCalculator,
            paymentAddressParser,
            syncManager,
            purpose,
            peerManager,
            dustCalculator,
            pluginManager,
            connectionManager
        )

        dataProvider.listener = bitcoinCore
        syncManager.listener = bitcoinCore

        val watchedTransactionManager = WatchedTransactionManager()
        bloomFilterManager.addBloomFilterProvider(watchedTransactionManager)
        bloomFilterManager.addBloomFilterProvider(bloomFilterProvider)
        bloomFilterManager.addBloomFilterProvider(pendingOutpointsProvider)
        bloomFilterManager.addBloomFilterProvider(irregularOutputFinder)

        bitcoinCore.watchedTransactionManager = watchedTransactionManager
        pendingTransactionProcessor.transactionListener = watchedTransactionManager
        blockTransactionProcessor.transactionListener = watchedTransactionManager

        bitcoinCore.peerGroup = peerGroup
        bitcoinCore.transactionSyncer = pendingTransactionSyncer
        bitcoinCore.networkMessageParser = networkMessageParser
        bitcoinCore.networkMessageSerializer = networkMessageSerializer
        bitcoinCore.unspentOutputSelector = unspentOutputSelector

        peerGroup.peerTaskHandler = bitcoinCore.peerTaskHandlerChain
        peerGroup.inventoryItemsHandler = bitcoinCore.inventoryItemsHandlerChain

        bitcoinCore.prependAddressConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))

        // this part can be moved to another place

        bitcoinCore.addMessageParser(AddrMessageParser())
            .addMessageParser(MerkleBlockMessageParser(BlockHeaderParser(blockHeaderHasher)))
            .addMessageParser(InvMessageParser())
            .addMessageParser(GetDataMessageParser())
            .addMessageParser(PingMessageParser())
            .addMessageParser(PongMessageParser())
            .addMessageParser(TransactionMessageParser())
            .addMessageParser(VerAckMessageParser())
            .addMessageParser(VersionMessageParser())
            .addMessageParser(RejectMessageParser())

        bitcoinCore.addMessageSerializer(FilterLoadMessageSerializer())
            .addMessageSerializer(GetBlocksMessageSerializer())
            .addMessageSerializer(InvMessageSerializer())
            .addMessageSerializer(GetDataMessageSerializer())
            .addMessageSerializer(MempoolMessageSerializer())
            .addMessageSerializer(PingMessageSerializer())
            .addMessageSerializer(PongMessageSerializer())
            .addMessageSerializer(TransactionMessageSerializer())
            .addMessageSerializer(VerAckMessageSerializer())
            .addMessageSerializer(VersionMessageSerializer())

        val bloomFilterLoader = BloomFilterLoader(bloomFilterManager, peerManager)
        bloomFilterManager.listener = bloomFilterLoader
        bitcoinCore.addPeerGroupListener(bloomFilterLoader)

        // todo: now this part cannot be moved to another place since bitcoinCore requires initialBlockDownload to be set. find solution to do so
        bitcoinCore.initialDownload = initialDownload
        bitcoinCore.addPeerTaskHandler(initialDownload)
        bitcoinCore.addInventoryItemsHandler(initialDownload)
        bitcoinCore.addPeerGroupListener(initialDownload)


        val mempoolTransactions = MempoolTransactions(pendingTransactionSyncer, transactionSender)
        bitcoinCore.addPeerTaskHandler(mempoolTransactions)
        bitcoinCore.addInventoryItemsHandler(mempoolTransactions)
        bitcoinCore.addPeerGroupListener(mempoolTransactions)

        transactionSender?.let {
            bitcoinCore.addPeerSyncListener(SendTransactionsOnPeersSynced(transactionSender))
            bitcoinCore.addPeerTaskHandler(transactionSender)
        }

        transactionSizeCalculator?.let {
            bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelector(transactionSizeCalculator, unspentOutputProvider))
            bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelectorSingleNoChange(transactionSizeCalculator, unspentOutputProvider))
        }

        return bitcoinCore
    }
}
