package com.wallet0x.bitcoincore

import com.wallet0x.bitcoincore.apisync.legacy.IPublicKeyFetcher
import com.wallet0x.bitcoincore.core.IPublicKeyManager
import com.wallet0x.bitcoincore.managers.AccountPublicKeyManager
import com.wallet0x.bitcoincore.managers.BloomFilterManager
import com.wallet0x.bitcoincore.managers.IBloomFilterProvider
import com.wallet0x.bitcoincore.managers.RestoreKeyConverterChain
import com.wallet0x.bitcoincore.models.PublicKey
import com.wallet0x.bitcoincore.models.WatchAddressPublicKey

class WatchAddressPublicKeyManager(
    private val publicKey: WatchAddressPublicKey,
    private val restoreKeyConverter: RestoreKeyConverterChain
) : IPublicKeyFetcher, IPublicKeyManager, IBloomFilterProvider {

    override fun publicKeys(indices: IntRange, external: Boolean) = listOf(publicKey)

    override fun changePublicKey() = publicKey

    override fun receivePublicKey() = publicKey

    override fun usedExternalPublicKeys(): List<PublicKey> = listOf(publicKey)

    override fun fillGap() {
        bloomFilterManager?.regenerateBloomFilter()
    }

    override fun addKeys(keys: List<PublicKey>) = Unit

    override fun gapShifts(): Boolean = false

    override fun getPublicKeyByPath(path: String): PublicKey {
        throw AccountPublicKeyManager.Error.InvalidPath
    }

    override var bloomFilterManager: BloomFilterManager? = null

    override fun getBloomFilterElements() = restoreKeyConverter.bloomFilterElements(publicKey)
}
