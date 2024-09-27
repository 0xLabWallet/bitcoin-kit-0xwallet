package com.wallet0x.dashkit.instantsend.instantsendlock

import com.wallet0x.bitcoincore.core.HashBytes
import com.wallet0x.dashkit.instantsend.InstantSendLockValidator
import com.wallet0x.dashkit.messages.ISLockMessage

class InstantSendLockManager(private val instantSendLockValidator: InstantSendLockValidator) {
    private val relayedLocks = mutableMapOf<HashBytes, com.wallet0x.dashkit.messages.ISLockMessage>()

    fun add(relayed: com.wallet0x.dashkit.messages.ISLockMessage) {
        relayedLocks[HashBytes(relayed.txHash)] = relayed
    }

    fun takeRelayedLock(txHash: ByteArray): com.wallet0x.dashkit.messages.ISLockMessage? {
        relayedLocks[HashBytes(txHash)]?.let {
            relayedLocks.remove(HashBytes(txHash))
            return it
        }
        return null
    }

    @Throws
    fun validate(isLock: com.wallet0x.dashkit.messages.ISLockMessage) {
        instantSendLockValidator.validate(isLock)
    }

}
