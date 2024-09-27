package com.wallet0x.bitcoincore.transactions

import com.wallet0x.bitcoincore.blocks.IPeerSyncListener

class SendTransactionsOnPeersSynced(var transactionSender: TransactionSender) : IPeerSyncListener {

    override fun onAllPeersSynced() {
        transactionSender.sendPendingTransactions()
    }

}

