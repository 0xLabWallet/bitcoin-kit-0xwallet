package com.wallet0x.dashkit.models

class InstantTransactionState {
    var instantTransactionHashes = mutableListOf<ByteArray>()

    fun append(hash: ByteArray) {
        instantTransactionHashes.add(hash)
    }
}
