package com.wallet0x.bitcoincore.apisync

import com.wallet0x.bitcoincore.apisync.blockchair.BlockchairBlockHashFetcher
import com.wallet0x.bitcoincore.apisync.blockchair.IBlockHashFetcher

class BlockHashFetcher(
    private val hsBlockHashFetcher: HsBlockHashFetcher,
    private val blockchairBlockHashFetcher: BlockchairBlockHashFetcher,
    private val checkpointHeight: Int
) : IBlockHashFetcher {

    override fun fetch(heights: List<Int>): Map<Int, String> {
        val beforeCheckpoint = heights.filter { it <= checkpointHeight }
        val afterCheckpoint = heights.filter { it > checkpointHeight }

        val blockHashes = mutableMapOf<Int, String>()
        if (beforeCheckpoint.isNotEmpty()) {
            blockHashes += hsBlockHashFetcher.fetch(beforeCheckpoint)
        }
        if (afterCheckpoint.isNotEmpty()) {
            blockHashes += blockchairBlockHashFetcher.fetch(afterCheckpoint)
        }

        return blockHashes
    }

}
