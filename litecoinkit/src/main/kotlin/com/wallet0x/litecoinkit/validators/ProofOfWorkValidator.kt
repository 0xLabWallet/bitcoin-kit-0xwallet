package com.wallet0x.litecoinkit.validators

import com.wallet0x.bitcoincore.blocks.validators.BlockValidatorException
import com.wallet0x.bitcoincore.blocks.validators.IBlockChainedValidator
import com.wallet0x.bitcoincore.crypto.CompactBits
import com.wallet0x.bitcoincore.extensions.toHexString
import com.wallet0x.bitcoincore.io.BitcoinOutput
import com.wallet0x.bitcoincore.models.Block
import com.wallet0x.litecoinkit.ScryptHasher
import java.math.BigInteger

class ProofOfWorkValidator(private val scryptHasher: ScryptHasher) : IBlockChainedValidator {

    override fun validate(block: Block, previousBlock: Block) {
        val blockHeaderData = getSerializedBlockHeader(block)

        val powHash = scryptHasher.hash(blockHeaderData).toHexString()

        check(BigInteger(powHash, 16) < CompactBits.decode(block.bits)) {
            throw BlockValidatorException.InvalidProofOfWork()
        }
    }

    private fun getSerializedBlockHeader(block: Block): ByteArray {
        return BitcoinOutput()
                .writeInt(block.version)
                .write(block.previousBlockHash)
                .write(block.merkleRoot)
                .writeUnsignedInt(block.timestamp)
                .writeUnsignedInt(block.bits)
                .writeUnsignedInt(block.nonce)
                .toByteArray()
    }

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

}
