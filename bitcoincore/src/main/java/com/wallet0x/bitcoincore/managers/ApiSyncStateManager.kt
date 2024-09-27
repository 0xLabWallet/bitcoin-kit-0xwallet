package com.wallet0x.bitcoincore.managers

import com.wallet0x.bitcoincore.core.IStorage

class ApiSyncStateManager(
        private val storage: IStorage,
        private val restoreFromApi: Boolean
) {

    var restored: Boolean
        get() {
            if (!restoreFromApi) {
                return true
            }

            return storage.initialRestored ?: false
        }
        set(value) {
            storage.setInitialRestored(value)
        }
}
