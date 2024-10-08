package com.wallet0x.bitcoincore.core

import com.wallet0x.bitcoincore.models.PublicKey
import com.wallet0x.hdwalletkit.HDKey
import com.wallet0x.hdwalletkit.HDWallet.*
import com.wallet0x.hdwalletkit.HDWalletAccount

class AccountWallet(private val hdWallet: HDWalletAccount, override val gapLimit: Int): IPrivateWallet, IAccountWallet {

    override fun publicKey(index: Int, external: Boolean): PublicKey {
        val pubKey = hdWallet.publicKey(index, if (external) Chain.EXTERNAL else Chain.INTERNAL)
        return PublicKey(0, index, external, pubKey.publicKey, pubKey.publicKeyHash)
    }

    override fun publicKeys(indices: IntRange, external: Boolean): List<PublicKey> {
        val hdPublicKeys = hdWallet.publicKeys(indices, if (external) Chain.EXTERNAL else Chain.INTERNAL)

        if (hdPublicKeys.size != indices.count()) {
            throw Wallet.HDWalletError.PublicKeysDerivationFailed()
        }

        return indices.mapIndexed { position, index ->
            val hdPublicKey = hdPublicKeys[position]
            PublicKey(0, index, external, hdPublicKey.publicKey, hdPublicKey.publicKeyHash)
        }
    }

    override fun privateKey(account: Int, index: Int, external: Boolean): HDKey {
       return hdWallet.privateKey(index, if (external) Chain.EXTERNAL else Chain.INTERNAL)
    }
}
