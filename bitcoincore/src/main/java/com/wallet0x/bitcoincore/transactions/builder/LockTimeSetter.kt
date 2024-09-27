package com.wallet0x.bitcoincore.transactions.builder

import com.wallet0x.bitcoincore.core.IStorage

class LockTimeSetter(private val storage: IStorage) {

    fun setLockTime(transaction: MutableTransaction) {
        transaction.transaction.lockTime = storage.lastBlock()?.height?.toLong() ?: 0
    }

}
