package com.wallet0x.bitcoincore.core

import com.wallet0x.bitcoincore.models.TransactionInfo
import com.wallet0x.bitcoincore.storage.FullTransactionInfo

class TransactionInfoConverter : ITransactionInfoConverter {
    override lateinit var baseConverter: BaseTransactionInfoConverter

    override fun transactionInfo(fullTransactionInfo: FullTransactionInfo): TransactionInfo {
        return baseConverter.transactionInfo(fullTransactionInfo)
    }
}
