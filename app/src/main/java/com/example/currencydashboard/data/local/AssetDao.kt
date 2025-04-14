package com.example.currencydashboard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets")
    fun getAllAssets(): Flow<List<AssetEntity>>
    
    @Query("SELECT * FROM assets WHERE isEnabled = 1")
    fun getEnabledAssets(): Flow<List<AssetEntity>>
    
    @Query("SELECT code FROM assets WHERE isEnabled = 1")
    suspend fun getEnabledAssetCodes(): List<String>
    
    @Query("SELECT * FROM assets WHERE code = :code")
    fun getAssetByCode(code: String): Flow<AssetEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)
    
    @Update
    suspend fun updateAsset(asset: AssetEntity)
    
    @Query("UPDATE assets SET rate = :rate, change = :change, lastUpdated = :timestamp WHERE code = :code")
    suspend fun updateRate(code: String, rate: Double, change: Double, timestamp: Long)
    
    @Query("UPDATE assets SET isEnabled = :isEnabled WHERE code = :code")
    suspend fun toggleAssetEnabled(code: String, isEnabled: Boolean)
} 