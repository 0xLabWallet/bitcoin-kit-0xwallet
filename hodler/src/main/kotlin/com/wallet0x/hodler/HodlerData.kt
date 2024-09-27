package com.wallet0x.hodler

import com.wallet0x.bitcoincore.core.IPluginData

data class HodlerData(val lockTimeInterval: LockTimeInterval) : IPluginData
