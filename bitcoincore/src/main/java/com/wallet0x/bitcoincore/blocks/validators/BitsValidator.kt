package com.wallet0x.bitcoincore.blocks.validators

import com.wallet0x.bitcoincore.models.Block

class BitsValidator : IBlockChainedValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        if (block.bits != previousBlock.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

}
