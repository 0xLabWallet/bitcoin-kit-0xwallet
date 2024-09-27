package com.wallet0x.bitcoincore.apisync.blockchair

import com.wallet0x.bitcoincore.apisync.model.BlockHeaderItem

class BlockchairLastBlockProvider(
    private val blockchairApi: BlockchairApi
) {
    fun lastBlockHeader(): BlockHeaderItem {
        return blockchairApi.lastBlockHeader()
    }
}
