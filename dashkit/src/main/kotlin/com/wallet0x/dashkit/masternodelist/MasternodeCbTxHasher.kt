package com.wallet0x.dashkit.masternodelist

import com.wallet0x.bitcoincore.core.HashBytes
import com.wallet0x.bitcoincore.core.IHasher
import com.wallet0x.dashkit.models.CoinbaseTransaction
import com.wallet0x.dashkit.models.CoinbaseTransactionSerializer

class MasternodeCbTxHasher(private val coinbaseTransactionSerializer: CoinbaseTransactionSerializer, private val hasher: IHasher) {

    fun hash(coinbaseTransaction: CoinbaseTransaction): HashBytes {
        val serialized = coinbaseTransactionSerializer.serialize(coinbaseTransaction)

        return HashBytes(hasher.hash(serialized))
    }

}
