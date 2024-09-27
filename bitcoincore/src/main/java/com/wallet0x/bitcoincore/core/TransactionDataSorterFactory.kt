package com.wallet0x.bitcoincore.core

import com.wallet0x.bitcoincore.models.TransactionDataSortType
import com.wallet0x.bitcoincore.utils.Bip69Sorter
import com.wallet0x.bitcoincore.utils.ShuffleSorter
import com.wallet0x.bitcoincore.utils.StraightSorter

class TransactionDataSorterFactory : ITransactionDataSorterFactory {
    override fun sorter(type: TransactionDataSortType): ITransactionDataSorter {
        return when (type) {
            TransactionDataSortType.None -> StraightSorter()
            TransactionDataSortType.Shuffle -> ShuffleSorter()
            TransactionDataSortType.Bip69 -> Bip69Sorter()
        }
    }
}
