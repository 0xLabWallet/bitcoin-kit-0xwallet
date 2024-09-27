package com.wallet0x.dashkit.core

import com.wallet0x.bitcoincore.core.IHasher
import com.wallet0x.bitcoincore.utils.HashUtils

class SingleSha256Hasher : IHasher {
    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.sha256(data)
    }
}