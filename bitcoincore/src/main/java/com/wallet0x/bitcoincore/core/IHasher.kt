package com.wallet0x.bitcoincore.core

interface IHasher {
    fun hash(data: ByteArray) : ByteArray
}
