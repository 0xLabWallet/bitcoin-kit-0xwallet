package com.wallet0x.dashkit

// TODO Rename to listener
interface IInstantTransactionDelegate {
    fun onUpdateInstant(transactionHash: ByteArray)
}
