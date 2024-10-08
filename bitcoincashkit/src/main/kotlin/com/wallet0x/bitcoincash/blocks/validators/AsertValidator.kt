package com.wallet0x.bitcoincash.blocks.validators

import com.wallet0x.bitcoincash.blocks.AsertAlgorithm
import com.wallet0x.bitcoincore.blocks.validators.BlockValidatorException
import com.wallet0x.bitcoincore.blocks.validators.IBlockChainedValidator
import com.wallet0x.bitcoincore.models.Block

class AsertValidator : IBlockChainedValidator {
    private val anchorBlockHeight = 661647
    private val anchorParentBlockTime = 1605447844.toBigInteger() // 2020 November 15, 14:13 GMT
    private val anchorBlockBits = 0x1804dafe

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return previousBlock.height >= anchorBlockHeight
    }

    override fun validate(block: Block, previousBlock: Block) {
        val bits = AsertAlgorithm.computeAsertTarget(anchorBlockBits, anchorParentBlockTime, anchorBlockHeight.toBigInteger(), previousBlock.timestamp.toBigInteger(), previousBlock.height.toBigInteger())
        if (bits != block.bits.toBigInteger()) {
            throw BlockValidatorException.NotEqualBits()
        }
    }
}
