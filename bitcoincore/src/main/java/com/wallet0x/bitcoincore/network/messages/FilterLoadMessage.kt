package com.wallet0x.bitcoincore.network.messages

import com.wallet0x.bitcoincore.crypto.BloomFilter

class FilterLoadMessage(bloomFilter: BloomFilter) : IMessage {
    var filter: BloomFilter = bloomFilter

    override fun toString(): String {
        return "FilterLoadMessage($filter)"
    }
}

class FilterLoadMessageSerializer : IMessageSerializer {
    override val command: String = "filterload"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is FilterLoadMessage) {
            return null
        }

        return message.filter.toByteArray()
    }
}
