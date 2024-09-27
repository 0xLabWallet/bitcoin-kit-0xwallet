package com.wallet0x.bitcoincore.transactions.builder

import com.wallet0x.bitcoincore.crypto.schnorr.Schnorr
import com.wallet0x.hdwalletkit.ECKey

fun ECKey.signSchnorr(input: ByteArray, auxRand: ByteArray = ByteArray(32)): ByteArray {
    return Schnorr.sign(input, privKeyBytes, auxRand)
}

fun ECKey.verifySchnorr(input: ByteArray, signature: ByteArray): Boolean {
    return Schnorr.verify(input, pubKeyXCoord, signature)
}
