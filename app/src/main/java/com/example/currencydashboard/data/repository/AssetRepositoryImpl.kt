package com.example.currencydashboard.data.repository

import com.example.currencydashboard.R
import com.example.currencydashboard.data.local.AssetDao
import com.example.currencydashboard.data.local.AssetEntity
import com.example.currencydashboard.data.local.PreferencesManager
import com.example.currencydashboard.data.local.toDomain
import com.example.currencydashboard.data.local.toEntity
import com.example.currencydashboard.data.remote.ExchangeRateDataSource
import com.example.currencydashboard.domain.model.Asset
import com.example.currencydashboard.domain.repository.AssetRepository
import com.example.currencydashboard.presentation.common.UiError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class AssetRepositoryImpl(
    private val assetDao: AssetDao,
    private val dataSource: ExchangeRateDataSource,
    private val preferencesManager: PreferencesManager
) : AssetRepository {
    
    // Base currency used for exchange rates
    // In the future, this can be read from storage preferences
    private val baseCurrency = "USD"
    
    override fun getAllAssets(): Flow<List<Asset>> {
        return assetDao.getAllAssets().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun initializeIfEmpty() {
        val assets = assetDao.getAllAssets().first()
        if (assets.isEmpty()) {
            refreshAssets()
        }
    }
    
    override fun getEnabledAssets(): Flow<List<Asset>> {
        return assetDao.getEnabledAssets().map { list ->
            list.map { it.toDomain() }
        }
    }
    
    override fun getLastRefreshTimestampFlow(): Flow<Long> {
        return preferencesManager.lastRefreshTimestampFlow
    }
    
    override suspend fun refreshRates(): Result<Unit> {
        return dataSource.getLiveRates(baseCurrency).fold(
            onSuccess = { response ->
                if (response.success) {
                    // Parse rates and update DB
                    val timestamp = System.currentTimeMillis()
                    preferencesManager.saveLastRefreshTimestamp(timestamp)
                    
                    // Get only the enabled assets to update their rates
                    val enabledCodes = assetDao.getEnabledAssetCodes()
                    
                    if (enabledCodes.isNotEmpty()) {
                        response.quotes.forEach { (quotePair, rate) ->
                            // Strip source currency from the pair (e.g., USDEUR -> EUR)
                            val targetCurrency = quotePair.substring(baseCurrency.length)
                            
                            // Only update rates for enabled assets
                            if (enabledCodes.contains(targetCurrency)) {
                                // For now using 0.0 for change as we don't have historical data
                                assetDao.updateRate(targetCurrency, rate, 0.0, timestamp)
                            }
                        }
                    }
                    
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("API call was not successful"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
    
    override suspend fun refreshAssetList(searchQuery: String): List<Asset> {
        return dataSource.getCurrencyList(searchQuery).fold(
            onSuccess = { response ->
                if (response.success) {
                    val allAssets = response.currencies.map { (code, name) ->
                        Asset(
                            code = code,
                            name = name,
                            isEnabled = false
                        )
                    }

                    allAssets
                } else {
                    getDefaultAssets()
                }
            },
            onFailure = { _ ->
                getDefaultAssets()
            }
        )
    }
    
    override suspend fun toggleAsset(code: String, isEnabled: Boolean): Result<Unit> {
        return runCatching {
            assetDao.toggleAssetEnabled(code, isEnabled)
            
            if (isEnabled) {
                refreshAssetRate(code)
            }
        }
    }
    
    override fun getAssetByCode(code: String): Flow<Asset?> {
        return assetDao.getAssetByCode(code).map { it?.toDomain() }
    }
    
    /**
     * Get all assets by merging cached assets with fresh API assets.
     * This ensures we have all possible assets while preserving their enabled state.
     * @param searchQuery Optional search query to filter assets by code or name
     */
    override suspend fun getMergedAssets(searchQuery: String): List<Asset> {
        // Make sure we have initial data if DB is empty
        initializeIfEmpty()
        
        // Get cached assets to preserve their state
        val cachedAssets = getAllAssets().first()
        
        // Force refresh the asset list from API to get all possible assets
        val allApiAssets = refreshAssetList(searchQuery)
        
        // Create a map of cached assets for quick lookup
        val cachedAssetMap = cachedAssets.associateBy { it.code }
        
        return allApiAssets.map { apiAsset ->
            cachedAssetMap[apiAsset.code]?.copy() ?: apiAsset
        }
    }
    
    /**
     * Fetch and update the rate for a specific asset
     */
    private suspend fun refreshAssetRate(code: String): Result<Unit> {
        return dataSource.getLiveRates(baseCurrency).fold(
            onSuccess = { response ->
                if (response.success) {
                    val timestamp = System.currentTimeMillis()
                    
                    // Look for this specific currency in the response
                    val quotePair = "$baseCurrency$code"
                    val rate = response.quotes[quotePair]
                    
                    if (rate != null) {
                        assetDao.updateRate(code, rate, 0.0, timestamp)
                        Result.success(Unit)
                    } else {
                        Result.failure(IOException("Rate not found for $code"))
                    }
                } else {
                    Result.failure(IOException("API call was not successful"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
    
    private suspend fun refreshAssets(): Result<Unit> {
        return dataSource.getCurrencyList().fold(
            onSuccess = { response ->
                if (response.success) {
                    val defaultAssets = getDefaultAssets()
                    
                    val assets = response.currencies.map { (code, name) ->
                        AssetEntity(
                            code = code,
                            name = name,
                            isEnabled = defaultAssets.any { it.code == code }
                        )
                    }
                    
                    assetDao.insertAssets(assets)
                    
                    // Only refresh rates for enabled assets
                    refreshRates()
                    
                    Result.success(Unit)
                } else {
                    insertDefaultAssets()
                }
            },
            onFailure = { _ ->
                insertDefaultAssets()
            }
        )
    }
    
    /**
     * Get a list of default assets
     */
    private fun getDefaultAssets(): List<Asset> {
        return listOf(
            Asset(baseCurrency, "US Dollar", false),
            Asset("EUR", "Euro", false)
        )
    }
    
    private suspend fun insertDefaultAssets(): Result<Unit> {
        val defaultAssets = getDefaultAssets()
        
        assetDao.insertAssets(defaultAssets.map { it.toEntity() })
        
        val enabledCodes = defaultAssets.filter { it.isEnabled }.map { it.code }
        if (enabledCodes.isNotEmpty()) {
            refreshRates()
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Get the currently set base currency
     */
    override fun getBaseCurrency(): String {
        return baseCurrency
    }
} 