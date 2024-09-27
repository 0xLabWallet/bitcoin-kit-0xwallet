package com.wallet0x.bitcoincore.apisync

import com.wallet0x.bitcoincore.apisync.model.TransactionItem
import com.wallet0x.bitcoincore.core.IApiTransactionProvider
import com.wallet0x.bitcoincore.managers.ApiSyncStateManager

class BiApiTransactionProvider(
    private val restoreProvider: IApiTransactionProvider,
    private val syncProvider: IApiTransactionProvider,
    private val syncStateManager: ApiSyncStateManager
) : IApiTransactionProvider {

    override fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem> =
        if (syncStateManager.restored) {
            syncProvider.transactions(addresses, stopHeight)
        } else {
            restoreProvider.transactions(addresses, stopHeight)
        }
}
