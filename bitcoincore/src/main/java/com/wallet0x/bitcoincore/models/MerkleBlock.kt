package com.wallet0x.bitcoincore.models

import com.wallet0x.bitcoincore.core.HashBytes
import com.wallet0x.bitcoincore.storage.BlockHeader
import com.wallet0x.bitcoincore.storage.FullTransaction

class MerkleBlock(val header: BlockHeader, val associatedTransactionHashes: Map<HashBytes, Boolean>) {

    var height: Int? = null
    var associatedTransactions = mutableListOf<FullTransaction>()
    val blockHash = header.hash

    val complete: Boolean
        get() = associatedTransactionHashes.size == associatedTransactions.size

}
