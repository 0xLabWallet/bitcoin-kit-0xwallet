package com.wallet0x.dashkit.messages

import com.wallet0x.bitcoincore.extensions.toReversedHex
import com.wallet0x.bitcoincore.io.BitcoinInputMarkable
import com.wallet0x.bitcoincore.network.messages.IMessage
import com.wallet0x.bitcoincore.network.messages.IMessageParser
import com.wallet0x.bitcoincore.serializers.TransactionSerializer
import com.wallet0x.bitcoincore.storage.FullTransaction

class TransactionLockMessage(var transaction: FullTransaction) : IMessage {
    override fun toString(): String {
        return "TransactionLockMessage(${transaction.header.hash.toReversedHex()})"
    }
}

class TransactionLockMessageParser : IMessageParser {
    override val command: String = "ix"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val transaction = TransactionSerializer.deserialize(input)
        return TransactionLockMessage(transaction)
    }
}
