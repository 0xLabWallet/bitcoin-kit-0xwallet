package com.wallet0x.bitcoincore.transactions

import com.wallet0x.bitcoincore.core.IPluginData
import com.wallet0x.bitcoincore.core.IPublicKeyManager
import com.wallet0x.bitcoincore.core.IRecipientSetter
import com.wallet0x.bitcoincore.models.TransactionDataSortType
import com.wallet0x.bitcoincore.storage.UnspentOutput
import com.wallet0x.bitcoincore.transactions.builder.InputSetter
import com.wallet0x.bitcoincore.transactions.builder.MutableTransaction
import com.wallet0x.bitcoincore.transactions.scripts.ScriptType
import com.wallet0x.bitcoincore.utils.AddressConverterChain

class TransactionFeeCalculator(
    private val recipientSetter: IRecipientSetter,
    private val inputSetter: InputSetter,
    private val addressConverter: AddressConverterChain,
    private val publicKeyManager: IPublicKeyManager,
    private val changeScriptType: ScriptType,
    private val transactionSizeCalculator: TransactionSizeCalculator
) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: String?, pluginData: Map<Byte, IPluginData>): Long {
        val mutableTransaction = MutableTransaction()

        recipientSetter.setRecipient(mutableTransaction, toAddress ?: sampleAddress(), value, pluginData, true)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay, TransactionDataSortType.None)

        val inputsTotalValue = mutableTransaction.inputsToSign.map { it.previousOutput.value }.sum()
        val outputsTotalValue = mutableTransaction.recipientValue + mutableTransaction.changeValue

        return inputsTotalValue - outputsTotalValue
    }

    fun fee(
        unspentOutputs: List<UnspentOutput>,
        feeRate: Int,
        toAddress: String?,
        pluginData: Map<Byte, IPluginData>
    ): Long {
        val mutableTransaction = MutableTransaction()

        val value = unspentOutputs.sumOf { it.output.value }

        recipientSetter.setRecipient(mutableTransaction, toAddress ?: sampleAddress(), value, pluginData, true)
        val transactionSize =
            transactionSizeCalculator.transactionSize(unspentOutputs.map { it.output }, listOf(mutableTransaction.recipientAddress.scriptType), mutableTransaction.getPluginDataOutputSize())

        return transactionSize * feeRate
    }

    private fun sampleAddress(): String {
        return addressConverter.convert(publicKey = publicKeyManager.changePublicKey(), scriptType = changeScriptType).stringValue
    }
}
