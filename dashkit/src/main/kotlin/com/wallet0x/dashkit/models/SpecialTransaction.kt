package com.wallet0x.dashkit.models

import com.wallet0x.bitcoincore.storage.FullTransaction

class SpecialTransaction(
        val transaction: FullTransaction,
        extraPayload: ByteArray,
        forceHashUpdate: Boolean = true
): FullTransaction(transaction.header, transaction.inputs, transaction.outputs, forceHashUpdate)
