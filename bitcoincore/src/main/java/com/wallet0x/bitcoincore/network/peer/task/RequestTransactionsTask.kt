package com.wallet0x.bitcoincore.network.peer.task

import com.wallet0x.bitcoincore.models.InventoryItem
import com.wallet0x.bitcoincore.network.messages.GetDataMessage
import com.wallet0x.bitcoincore.network.messages.IMessage
import com.wallet0x.bitcoincore.network.messages.TransactionMessage
import com.wallet0x.bitcoincore.storage.FullTransaction

class RequestTransactionsTask(hashes: List<ByteArray>) : PeerTask() {
    val hashes = hashes.toMutableList()
    var transactions = mutableListOf<FullTransaction>()

    override val state: String
        get() =
            "hashesCount: ${hashes.size}; receivedTransactionsCount: ${transactions.size}"

    override fun start() {
        val items = hashes.map { hash ->
            InventoryItem(InventoryItem.MSG_TX, hash)
        }

        requester?.send(GetDataMessage(items))
        resetTimer()
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message !is TransactionMessage) {
            return false
        }

        val transaction = message.transaction
        val hash = hashes.firstOrNull { it.contentEquals(transaction.header.hash) } ?: return false

        hashes.remove(hash)
        transactions.add(transaction)

        if (hashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

}
