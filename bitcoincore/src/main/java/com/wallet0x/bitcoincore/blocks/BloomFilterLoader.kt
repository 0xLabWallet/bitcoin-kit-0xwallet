package com.wallet0x.bitcoincore.blocks

import com.wallet0x.bitcoincore.crypto.BloomFilter
import com.wallet0x.bitcoincore.managers.BloomFilterManager
import com.wallet0x.bitcoincore.network.peer.Peer
import com.wallet0x.bitcoincore.network.peer.PeerGroup
import com.wallet0x.bitcoincore.network.peer.PeerManager

class BloomFilterLoader(private val bloomFilterManager: BloomFilterManager, private val peerManager: PeerManager)
    : PeerGroup.Listener, BloomFilterManager.Listener {

    override fun onPeerConnect(peer: Peer) {
        bloomFilterManager.bloomFilter?.let {
            peer.filterLoad(it)
        }
    }

    override fun onFilterUpdated(bloomFilter: BloomFilter) {
        peerManager.connected().forEach {
            it.filterLoad(bloomFilter)
        }
    }
}
