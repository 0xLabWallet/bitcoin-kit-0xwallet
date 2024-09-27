package com.wallet0x.dashkit.tasks

import com.wallet0x.bitcoincore.models.InventoryItem
import com.wallet0x.bitcoincore.network.messages.GetDataMessage
import com.wallet0x.bitcoincore.network.messages.IMessage
import com.wallet0x.bitcoincore.network.peer.task.PeerTask
import com.wallet0x.dashkit.InventoryType

class RequestInstantSendLocksTask(hashes: List<ByteArray>) : PeerTask() {

    val hashes = hashes.toMutableList()
    var isLocks = mutableListOf<com.wallet0x.dashkit.messages.ISLockMessage>()

    override fun start() {
        requester?.send(GetDataMessage(hashes.map { InventoryItem(InventoryType.MSG_ISLOCK, it) }))
    }

    override fun handleMessage(message: IMessage) = when (message) {
        is com.wallet0x.dashkit.messages.ISLockMessage -> handleISLockVote(message)
        else -> false
    }

    private fun handleISLockVote(isLockMessage: com.wallet0x.dashkit.messages.ISLockMessage): Boolean {
        val hash = hashes.firstOrNull { it.contentEquals(isLockMessage.hash) } ?: return false

        hashes.remove(hash)
        isLocks.add(isLockMessage)

        if (hashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

}
