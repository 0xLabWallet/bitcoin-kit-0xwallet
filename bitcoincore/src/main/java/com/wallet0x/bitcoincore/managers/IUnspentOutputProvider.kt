package com.wallet0x.bitcoincore.managers

import com.wallet0x.bitcoincore.storage.UnspentOutput

interface IUnspentOutputProvider {
    fun getSpendableUtxo(): List<UnspentOutput>
}
