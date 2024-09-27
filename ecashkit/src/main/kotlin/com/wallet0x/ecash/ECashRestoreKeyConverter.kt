package com.wallet0x.ecash

import com.wallet0x.bitcoincore.extensions.toHexString
import com.wallet0x.bitcoincore.managers.IRestoreKeyConverter
import com.wallet0x.bitcoincore.models.PublicKey

class ECashRestoreKeyConverter: IRestoreKeyConverter {
    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        return listOf(publicKey.publicKeyHash.toHexString())
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.publicKeyHash, publicKey.publicKey)
    }
}
