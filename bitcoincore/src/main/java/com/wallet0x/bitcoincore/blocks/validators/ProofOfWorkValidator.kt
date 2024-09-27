package com.wallet0x.bitcoincore.blocks.validators

import com.wallet0x.bitcoincore.crypto.CompactBits
import com.wallet0x.bitcoincore.extensions.toReversedHex
import com.wallet0x.bitcoincore.models.Block
import java.math.BigInteger

class ProofOfWorkValidator : IBlockChainedValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        check(BigInteger(block.headerHash.toReversedHex(), 16) < CompactBits.decode(block.bits)) {
            throw BlockValidatorException.InvalidProofOfWork()
        }
    }
}
