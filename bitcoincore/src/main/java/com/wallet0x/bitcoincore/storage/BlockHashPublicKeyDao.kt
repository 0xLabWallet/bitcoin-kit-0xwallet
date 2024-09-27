package com.wallet0x.bitcoincore.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.wallet0x.bitcoincore.models.BlockHashPublicKey

@Dao
interface BlockHashPublicKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<BlockHashPublicKey>)

}
