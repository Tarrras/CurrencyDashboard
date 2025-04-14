package com.example.currencydashboard.domain.repository

import com.example.currencydashboard.domain.model.Asset
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    /**
     * Get all available assets as a Flow
     */
    fun getAllAssets(): Flow<List<Asset>>

    /**
     * Initialize the database if it's empty
     */
    suspend fun initializeIfEmpty()

    /**
     * Get only enabled assets that should be displayed on the home screen
     */
    fun getEnabledAssets(): Flow<List<Asset>>

    /**
     * Get the last refresh timestamp as a Flow
     */
    fun getLastRefreshTimestampFlow(): Flow<Long>

    /**
     * Get latest exchange rates for all assets
     */
    suspend fun refreshRates(): Result<Unit>

    /**
     * Force refresh the complete list of available assets from the API
     * This ensures we have all possible assets, not just those in the cache
     * @param searchQuery Optional search query to filter assets by code or name
     * @return List of assets matching the search criteria
     */
    suspend fun refreshAssetList(searchQuery: String = ""): List<Asset>

    /**
     * Toggle asset enabled state
     */
    suspend fun toggleAsset(code: String, isEnabled: Boolean): Result<Unit>

    /**
     * Get asset by code
     */
    fun getAssetByCode(code: String): Flow<Asset?>

    /**
     * Get all assets by merging cached assets with fresh API assets.
     * This ensures we have all possible assets while preserving their enabled state.
     * @param searchQuery Optional search query to filter assets by code or name
     */
    suspend fun getMergedAssets(searchQuery: String = ""): List<Asset>

    /**
     * Get the currently set base currency
     * @return The currency code of the current base currency
     */
    fun getBaseCurrency(): String
} 