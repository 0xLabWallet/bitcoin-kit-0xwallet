package com.wallet0x.dashkit.tasks

import com.wallet0x.bitcoincore.models.InventoryItem
import com.wallet0x.bitcoincore.network.messages.GetDataMessage
import com.wallet0x.bitcoincore.network.messages.IMessage
import com.wallet0x.bitcoincore.network.peer.task.PeerTask
import com.wallet0x.bitcoincore.storage.FullTransaction
import com.wallet0x.dashkit.InventoryType
import com.wallet0x.dashkit.messages.TransactionLockMessage

class RequestTransactionLockRequestsTask(hashes: List<ByteArray>) : PeerTask() {

    val hashes = hashes.toMutableList()
    var transactions = mutableListOf<FullTransaction>()

    override fun start() {
        val items = hashes.map { hash ->
            InventoryItem(InventoryType.MSG_TXLOCK_REQUEST, hash)
        }

        requester?.send(GetDataMessage(items))
    }

    override fun handleMessage(message: IMessage) = when (message) {
        is TransactionLockMessage -> handleTransactionLockRequest(message.transaction)
        else -> false
    }

    private fun handleTransactionLockRequest(transaction: FullTransaction): Boolean {
        val hash = hashes.firstOrNull { it.contentEquals(transaction.header.hash) } ?: return false

        hashes.remove(hash)
        transactions.add(transaction)

        if (hashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

}
