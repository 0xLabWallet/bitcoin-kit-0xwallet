package com.wallet0x.bitcoincore.blocks.validators

import com.wallet0x.bitcoincore.models.Block

interface IBlockValidator {
    fun validate(block: Block, previousBlock: Block)
}

interface IBlockChainedValidator : IBlockValidator {
    fun isBlockValidatable(block: Block, previousBlock: Block): Boolean
}
