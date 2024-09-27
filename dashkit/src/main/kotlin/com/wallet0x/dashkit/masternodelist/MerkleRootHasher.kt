package com.wallet0x.dashkit.masternodelist

import com.wallet0x.bitcoincore.core.IHasher
import com.wallet0x.bitcoincore.utils.HashUtils
import com.wallet0x.dashkit.IMerkleHasher

class MerkleRootHasher: IHasher, IMerkleHasher {

    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.doubleSha256(data)
    }

    override fun hash(first: ByteArray, second: ByteArray): ByteArray {
        return HashUtils.doubleSha256(first + second)
    }
}
