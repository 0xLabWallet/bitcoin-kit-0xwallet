package com.wallet0x.bitcoincore.storage

import androidx.room.TypeConverter
import com.wallet0x.bitcoincore.extensions.hexToByteArray
import com.wallet0x.bitcoincore.extensions.toHexString

class WitnessConverter {

    @TypeConverter
    fun fromWitness(list: List<ByteArray>): String {
        return list.joinToString(", ") {
            it.toHexString()
        }
    }

    @TypeConverter
    fun toWitness(data: String): List<ByteArray> = when {
        data.isEmpty() -> listOf()
        else -> data.split(", ").map {
            it.hexToByteArray()
        }
    }
}
