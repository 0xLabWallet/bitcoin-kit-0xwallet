package com.wallet0x.bitcoincore.transactions.builder

import com.wallet0x.bitcoincore.core.IPrivateWallet
import com.wallet0x.bitcoincore.models.Transaction
import com.wallet0x.bitcoincore.models.TransactionOutput
import com.wallet0x.bitcoincore.serializers.TransactionSerializer
import com.wallet0x.bitcoincore.storage.InputToSign
import com.wallet0x.hdwalletkit.Utils

class SchnorrInputSigner(
    private val hdWallet: IPrivateWallet
) {
    fun sigScriptData(
        transaction: Transaction,
        inputsToSign: List<InputToSign>,
        outputs: List<TransactionOutput>,
        index: Int
    ): List<ByteArray> {
        val input = inputsToSign[index]
        val publicKey = input.previousOutputPublicKey
        val tweakedPrivateKey = checkNotNull(hdWallet.privateKey(publicKey.account, publicKey.index, publicKey.external).tweakedOutputKey) {
            throw Error.NoPrivateKey()
        }
        val serializedTransaction = TransactionSerializer.serializeForTaprootSignature(transaction, inputsToSign, outputs, index)

        val signatureHash = Utils.taggedHash("TapSighash", serializedTransaction)
        val signature = tweakedPrivateKey.signSchnorr(signatureHash)

        return listOf(signature)
    }

    open class Error : Exception() {
        class NoPrivateKey : Error()
        class NoPreviousOutput : Error()
        class NoPreviousOutputAddress : Error()
    }
}
