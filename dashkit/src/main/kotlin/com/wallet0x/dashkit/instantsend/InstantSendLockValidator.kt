package com.wallet0x.dashkit.instantsend

import com.wallet0x.bitcoincore.io.BitcoinOutput
import com.wallet0x.bitcoincore.utils.HashUtils
import com.wallet0x.dashkit.DashKitErrors
import com.wallet0x.dashkit.managers.QuorumListManager
import com.wallet0x.dashkit.models.QuorumType

class InstantSendLockValidator(
        private val quorumListManager: QuorumListManager,
        private val bls: BLS
) {

    @Throws
    fun validate(islock: com.wallet0x.dashkit.messages.ISLockMessage) {
        // 01. Select quorum
        val quorum = quorumListManager.getQuorum(QuorumType.LLMQ_50_60, islock.requestId)

        // 02. Make signId data to verify signature
        val signIdPayload = BitcoinOutput()
                .writeByte(quorum.type)
                .write(quorum.quorumHash)
                .write(islock.requestId)
                .write(islock.txHash)
                .toByteArray()

        val signId = HashUtils.doubleSha256(signIdPayload)

        // 03. Verify signature by BLS
        if (!bls.verifySignature(quorum.quorumPublicKey, islock.sign, signId)) {
            throw DashKitErrors.ISLockValidation.SignatureNotValid()
        }
    }
}
