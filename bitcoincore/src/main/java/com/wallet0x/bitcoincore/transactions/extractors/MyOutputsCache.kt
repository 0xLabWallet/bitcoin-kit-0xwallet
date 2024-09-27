package com.wallet0x.bitcoincore.transactions.extractors

import com.wallet0x.bitcoincore.core.HashBytes
import com.wallet0x.bitcoincore.core.IStorage
import com.wallet0x.bitcoincore.models.TransactionInput
import com.wallet0x.bitcoincore.models.TransactionOutput

class MyOutputsCache {
    private val outputsCache = mutableMapOf<HashBytes, MutableMap<Int, Long>>()

    fun add(outputs: List<TransactionOutput>) {
        for (output in outputs) {
            if (output.publicKeyPath != null) {
                if (!outputsCache.containsKey(HashBytes(output.transactionHash))) {
                    outputsCache[HashBytes(output.transactionHash)] = mutableMapOf()
                }

                outputsCache[HashBytes(output.transactionHash)]?.set(output.index, output.value)
            }
        }
    }

    fun valueSpentBy(input: TransactionInput): Long? {
        return outputsCache[HashBytes(input.previousOutputTxHash)]?.get(input.previousOutputIndex.toInt())
    }

    companion object {
        fun create(storage: IStorage): MyOutputsCache {
            val outputsCache = MyOutputsCache()
            val outputs = storage.getMyOutputs()

            outputsCache.add(outputs)

            return outputsCache
        }
    }
}
