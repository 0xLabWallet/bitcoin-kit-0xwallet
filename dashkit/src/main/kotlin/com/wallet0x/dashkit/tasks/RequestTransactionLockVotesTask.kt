package com.wallet0x.dashkit.tasks

import com.wallet0x.bitcoincore.models.InventoryItem
import com.wallet0x.bitcoincore.network.messages.GetDataMessage
import com.wallet0x.bitcoincore.network.messages.IMessage
import com.wallet0x.bitcoincore.network.peer.task.PeerTask
import com.wallet0x.dashkit.InventoryType
import com.wallet0x.dashkit.messages.TransactionLockVoteMessage

class RequestTransactionLockVotesTask(hashes: List<ByteArray>) : PeerTask() {

    val hashes = hashes.toMutableList()
    var transactionLockVotes = mutableListOf<TransactionLockVoteMessage>()

    override fun start() {
        val items = hashes.map { hash ->
            InventoryItem(InventoryType.MSG_TXLOCK_VOTE, hash)
        }

        requester?.send(GetDataMessage(items))
    }

    override fun handleMessage(message: IMessage) = when (message) {
        is TransactionLockVoteMessage -> handleTransactionLockVote(message)
        else -> false
    }

    private fun handleTransactionLockVote(transactionLockVote: TransactionLockVoteMessage): Boolean {
        val hash = hashes.firstOrNull { it.contentEquals(transactionLockVote.hash) } ?: return false

        hashes.remove(hash)
        transactionLockVotes.add(transactionLockVote)

        if (hashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

}
