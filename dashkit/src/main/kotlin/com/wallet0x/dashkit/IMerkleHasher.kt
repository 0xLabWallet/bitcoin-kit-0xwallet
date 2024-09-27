package com.wallet0x.dashkit

interface IMerkleHasher {
    fun hash(first: ByteArray, second: ByteArray) : ByteArray
}