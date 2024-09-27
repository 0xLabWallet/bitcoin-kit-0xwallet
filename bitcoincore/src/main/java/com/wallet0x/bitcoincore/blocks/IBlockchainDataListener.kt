package com.wallet0x.bitcoincore.blocks

import com.wallet0x.bitcoincore.models.Block
import com.wallet0x.bitcoincore.models.Transaction

interface IBlockchainDataListener {
    fun onBlockInsert(block: Block)
    fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, block: Block?)
    fun onTransactionsDelete(hashes: List<String>)
}
