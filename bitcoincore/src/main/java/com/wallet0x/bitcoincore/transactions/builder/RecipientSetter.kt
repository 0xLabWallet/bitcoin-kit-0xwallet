package com.wallet0x.bitcoincore.transactions.builder

import com.wallet0x.bitcoincore.core.IPluginData
import com.wallet0x.bitcoincore.core.IRecipientSetter
import com.wallet0x.bitcoincore.core.PluginManager
import com.wallet0x.bitcoincore.transactions.builder.MutableTransaction
import com.wallet0x.bitcoincore.utils.IAddressConverter

class RecipientSetter(
        private val addressConverter: IAddressConverter,
        private val pluginManager: PluginManager
) : IRecipientSetter {

    override fun setRecipient(mutableTransaction: MutableTransaction, toAddress: String, value: Long, pluginData: Map<Byte, IPluginData>, skipChecking: Boolean) {
        mutableTransaction.recipientAddress = addressConverter.convert(toAddress)
        mutableTransaction.recipientValue = value

        pluginManager.processOutputs(mutableTransaction, pluginData, skipChecking)
    }

}
