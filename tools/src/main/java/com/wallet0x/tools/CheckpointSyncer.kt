package com.wallet0x.tools

import com.wallet0x.bitcoincore.core.DoubleSha256Hasher
import com.wallet0x.bitcoincore.core.IConnectionManager
import com.wallet0x.bitcoincore.core.IConnectionManagerListener
import com.wallet0x.bitcoincore.extensions.toReversedHex
import com.wallet0x.bitcoincore.models.Block
import com.wallet0x.bitcoincore.network.Network
import com.wallet0x.bitcoincore.network.messages.*
import com.wallet0x.bitcoincore.network.peer.IPeerTaskHandler
import com.wallet0x.bitcoincore.network.peer.Peer
import com.wallet0x.bitcoincore.network.peer.PeerGroup
import com.wallet0x.bitcoincore.network.peer.PeerManager
import com.wallet0x.bitcoincore.network.peer.task.GetBlockHeadersTask
import com.wallet0x.bitcoincore.network.peer.task.PeerTask
import com.wallet0x.bitcoincore.storage.BlockHeader
import com.wallet0x.dashkit.MainNetDash
import com.wallet0x.dashkit.TestNetDash
import com.wallet0x.dashkit.X11Hasher
import java.util.*
import java.util.concurrent.Executors

class CheckpointSyncer(
        private val network: Network,
        private val checkpointInterval: Int,
        private val checkpointsToKeep: Int,
        private val listener: Listener
)
    : PeerGroup.Listener, IPeerTaskHandler {

    interface Listener {
        fun onSync(network: Network, checkpoints: List<Block>)
    }

    var isSynced: Boolean = false
        private set

    @Volatile
    private var syncPeer: Peer? = null
    private val peersQueue = Executors.newSingleThreadExecutor()
    private val peerManager = PeerManager()

    private val peerSize = 2
    private val peerGroup: PeerGroup

    private val lastCheckpointBlock = network.lastCheckpoint.block
    private val checkpoints = mutableListOf(lastCheckpointBlock)
    private val blocks = LinkedList<Block>().also {
        it.add(lastCheckpointBlock)
    }

    init {
        val blockHeaderHasher = when (network) {
            is TestNetDash,
            is MainNetDash -> X11Hasher()
            else -> DoubleSha256Hasher()
        }

        val networkMessageParser = NetworkMessageParser(network.magic).apply {
            add(VersionMessageParser())
            add(VerAckMessageParser())
            add(InvMessageParser())
            add(HeadersMessageParser(blockHeaderHasher))
        }

        val networkMessageSerializer = NetworkMessageSerializer(network.magic).apply {
            add(VersionMessageSerializer())
            add(VerAckMessageSerializer())
            add(InvMessageSerializer())
            add(GetHeadersMessageSerializer())
            add(HeadersMessageSerializer())
        }

        val connectionManager = object : IConnectionManager {
            override val listener: IConnectionManagerListener? = null
            override val isConnected = true

            override fun onEnterForeground() {
            }

            override fun onEnterBackground() {
            }
        }

        val peerHostManager = PeerAddressManager(network)
        peerGroup = PeerGroup(peerHostManager, network, peerManager, peerSize, networkMessageParser, networkMessageSerializer, connectionManager, 0, false).also {
            peerHostManager.listener = it
        }

        peerGroup.addPeerGroupListener(this)
        peerGroup.peerTaskHandler = this
    }

    fun start() {
        isSynced = false
        peerGroup.start()
    }

    //  PeerGroup Listener

    override fun onPeerConnect(peer: Peer) {
        assignNextSyncPeer()
    }

    override fun onPeerDisconnect(peer: Peer, e: Exception?) {
        if (peer == syncPeer) {
            syncPeer = null
            assignNextSyncPeer()
        }
    }

    override fun onPeerReady(peer: Peer) {
        if (peer == syncPeer) {
            downloadBlockchain()
        }
    }

    //  IPeerTaskHandler

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        if (task is GetBlockHeadersTask) {
            validateHeaders(peer, task.blockHeaders)
            return true
        }

        return false
    }

    private fun validateHeaders(peer: Peer, headers: Array<BlockHeader>) {
        var prevBlock = blocks.last()

        if (headers.size < 2000) {
            peer.synced = true
            downloadBlockchain()
            return
        }

        for (header in headers) {
            if (!prevBlock.headerHash.contentEquals(header.previousBlockHeaderHash)) {
                syncPeer = null
                assignNextSyncPeer()
                break
            }

            val newBlock = Block(header, prevBlock.height + 1)
            if (newBlock.height % checkpointInterval == 0) {
                print("Checkpoint block ${header.hash.toReversedHex()} at height ${newBlock.height}, time ${header.timestamp}")
                checkpoints.add(newBlock)
            }

            blocks.add(newBlock)
            prevBlock = newBlock
        }

        downloadBlockchain()
    }

    private fun assignNextSyncPeer() {
        peersQueue.execute {
            if (peerManager.connected().none { !it.synced }) {
                isSynced = true
                peerGroup.stop()
                print("Synced")

                val checkpoint = checkpoints.last()

                if (checkpointsToKeep == 1) {
                    listener.onSync(network, listOf(checkpoint))
                    return@execute
                }

                if (checkpoint.height <= blocks.last.height && blocks.size >= checkpointsToKeep) {
                    val filter = blocks.filter { it.height <= checkpoint.height }
                    listener.onSync(network, filter.takeLast(checkpointsToKeep).reversed())
                }

                return@execute
            }

            if (syncPeer == null) {
                val notSyncedPeers = peerManager.sorted().filter { !it.synced }
                notSyncedPeers.firstOrNull { it.ready }?.let { nonSyncedPeer ->
                    syncPeer = nonSyncedPeer

                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
        val peer = syncPeer
        if (peer == null || !peer.ready) {
            return
        }

        if (peer.synced) {
            syncPeer = null
            assignNextSyncPeer()
        } else {
            peer.addTask(GetBlockHeadersTask(getBlockLocatorHashes()))
        }
    }

    private fun getBlockLocatorHashes(): List<ByteArray> {
        return if (blocks.isEmpty()) {
            listOf(checkpoints.last().headerHash)
        } else {
            listOf(blocks.last().headerHash)
        }
    }

    private fun print(message: String) {
        println("${network.javaClass.simpleName}: $message")
    }
}
