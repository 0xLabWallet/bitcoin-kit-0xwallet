package com.wallet0x.bitcoincore.transactions.builder

import com.wallet0x.bitcoincore.core.IPrivateWallet
import com.wallet0x.bitcoincore.models.Transaction
import com.wallet0x.bitcoincore.models.TransactionOutput
import com.wallet0x.bitcoincore.network.Network
import com.wallet0x.bitcoincore.serializers.TransactionSerializer
import com.wallet0x.bitcoincore.storage.InputToSign
import com.wallet0x.bitcoincore.transactions.scripts.ScriptType

class EcdsaInputSigner(
    private val hdWallet: IPrivateWallet,
    private val network: Network
) {

    fun sigScriptData(transaction: Transaction, inputsToSign: List<InputToSign>, outputs: List<TransactionOutput>, index: Int): List<ByteArray> {

        val input = inputsToSign[index]
        val prevOutput = input.previousOutput
        val publicKey = input.previousOutputPublicKey

        val privateKey = checkNotNull(hdWallet.privateKey(publicKey.account, publicKey.index, publicKey.external)) {
            throw Error.NoPrivateKey()
        }

        val txContent = TransactionSerializer.serializeForSignature(
            transaction = transaction,
            inputsToSign = inputsToSign,
            outputs = outputs,
            inputIndex = index,
            isWitness = prevOutput.scriptType.isWitness || network.sigHashForked
        ) + byteArrayOf(network.sigHashValue, 0, 0, 0)
        val signature = privateKey.createSignature(txContent) + network.sigHashValue

        return when (prevOutput.scriptType) {
            ScriptType.P2PK -> listOf(signature)
            else -> listOf(signature, publicKey.publicKey)
        }
    }

    open class Error : Exception() {
        class NoPrivateKey : Error()
        class NoPreviousOutput : Error()
        class NoPreviousOutputAddress : Error()
    }
}
