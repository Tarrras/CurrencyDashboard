package com.example.currencydashboard.data.repository

import com.example.currencydashboard.data.local.AssetDao
import com.example.currencydashboard.data.local.AssetEntity
import com.example.currencydashboard.data.local.PreferencesManager
import com.example.currencydashboard.data.remote.CurrencyListResponse
import com.example.currencydashboard.data.remote.ExchangeRateDataSource
import com.example.currencydashboard.data.remote.LiveRatesResponse
import com.example.currencydashboard.domain.model.Asset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AssetRepositoryImplTest {
    
    private lateinit var assetDao: AssetDao
    private lateinit var dataSource: ExchangeRateDataSource
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: AssetRepositoryImpl
    
    @Before
    fun setup() {
        assetDao = mockk(relaxed = true)
        dataSource = mockk()
        preferencesManager = mockk(relaxed = true)
        
        every { preferencesManager.lastRefreshTimestampFlow } returns flowOf(123456789L)
        
        repository = AssetRepositoryImpl(assetDao, dataSource, preferencesManager)
    }
    
    @Test
    fun `getAllAssets should return Flow of assets from DAO`() = runTest {
        // Given
        val mockEntities = listOf(
            AssetEntity("USD", "US Dollar", true, 1.0),
            AssetEntity("EUR", "Euro", false, 0.85)
        )
        every { assetDao.getAllAssets() } returns flowOf(mockEntities)
        
        // When
        val result = repository.getAllAssets()
        
        // Then
        result.collect { assets ->
            assertEquals(2, assets.size)
            assertEquals("USD", assets[0].code)
            assertEquals("EUR", assets[1].code)
        }
        verify { assetDao.getAllAssets() }
    }
    
    @Test
    fun `getEnabledAssets should return Flow of enabled assets from DAO`() = runTest {
        // Given
        val mockEntities = listOf(
            AssetEntity("USD", "US Dollar", true, 1.0)
        )
        every { assetDao.getEnabledAssets() } returns flowOf(mockEntities)
        
        // When
        val result = repository.getEnabledAssets()
        
        // Then
        result.collect { assets ->
            assertEquals(1, assets.size)
            assertEquals("USD", assets[0].code)
            assertEquals(true, assets[0].isEnabled)
        }
        verify { assetDao.getEnabledAssets() }
    }
    
    @Test
    fun `getLastRefreshTimestampFlow should return Flow from PreferencesManager`() = runTest {
        // Given
        val expectedTimestamp = 123456789L
        every { preferencesManager.lastRefreshTimestampFlow } returns flowOf(expectedTimestamp)
        
        // When
        val result = repository.getLastRefreshTimestampFlow()
        
        // Then
        result.collect { timestamp ->
            assertEquals(expectedTimestamp, timestamp)
        }
        verify { preferencesManager.lastRefreshTimestampFlow }
    }
    
    @Test
    fun `refreshRates should only update enabled assets`() = runTest {
        // Given
        val enabledCodes = listOf("EUR", "BTC")
        coEvery { assetDao.getEnabledAssetCodes() } returns enabledCodes
        
        val response = LiveRatesResponse(
            success = true,
            timestamp = 123456789,
            source = "USD",
            quotes = mapOf(
                "USDEUR" to 0.85, 
                "USDGBP" to 0.75, 
                "USDBTC" to 0.000021
            )
        )
        coEvery { dataSource.getLiveRates() } returns Result.success(response)
        
        // When
        val result = repository.refreshRates()
        
        // Then
        assert(result.isSuccess)
        // Verify that only enabled assets were updated
        coVerify { assetDao.updateRate("EUR", 0.85, 0.0, any()) }
        coVerify { assetDao.updateRate("BTC", 0.000021, 0.0, any()) }
        // Verify that disabled asset was not updated
        coVerify(exactly = 0) { assetDao.updateRate("GBP", any(), any(), any()) }
        verify { preferencesManager.saveLastRefreshTimestamp(any()) }
    }
    
    @Test
    fun `refreshRates should return failure when API call fails`() = runTest {
        // Given
        val error = IOException("Network error")
        coEvery { dataSource.getLiveRates() } returns Result.failure(error)
        
        // When
        val result = repository.refreshRates()
        
        // Then
        assert(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
    
    @Test
    fun `toggleAsset should use direct DAO method and fetch rates when enabled`() = runTest {
        // Given
        val assetCode = "BTC"
        val isEnabled = true
        
        // Mock for refreshAssetRate
        val response = LiveRatesResponse(
            success = true,
            timestamp = 123456789,
            source = "USD",
            quotes = mapOf("USDBTC" to 0.000021)
        )
        coEvery { dataSource.getLiveRates() } returns Result.success(response)
        
        // When
        val result = repository.toggleAsset(assetCode, isEnabled)
        
        // Then
        assert(result.isSuccess)
        coVerify { assetDao.toggleAssetEnabled(assetCode, isEnabled) }
        // Verify that rate was fetched for the newly enabled asset
        coVerify { dataSource.getLiveRates() }
        coVerify { assetDao.updateRate("BTC", 0.000021, 0.0, any()) }
    }
    
    @Test
    fun `toggleAsset should not fetch rates when asset is disabled`() = runTest {
        // Given
        val assetCode = "BTC"
        val isEnabled = false
        
        // When
        val result = repository.toggleAsset(assetCode, isEnabled)
        
        // Then
        assert(result.isSuccess)
        coVerify { assetDao.toggleAssetEnabled(assetCode, isEnabled) }
        // Verify that rate was not fetched for the disabled asset
        coVerify(exactly = 0) { dataSource.getLiveRates() }
    }
    
    @Test
    fun `initializeIfEmpty should refresh assets when database is empty`() = runTest {
        // Given
        val emptyList = emptyList<AssetEntity>()
        every { assetDao.getAllAssets() } returns flowOf(emptyList)
        
        val currencyResponse = CurrencyListResponse(
            success = true,
            currencies = mapOf("USD" to "US Dollar", "EUR" to "Euro")
        )
        coEvery { dataSource.getCurrencyList() } returns Result.success(currencyResponse)
        coEvery { dataSource.getLiveRates() } returns Result.success(
            LiveRatesResponse(true, 12345, "USD", emptyMap())
        )
        
        // For refreshRates
        coEvery { assetDao.getEnabledAssetCodes() } returns listOf("USD", "EUR")
        
        // When
        repository.initializeIfEmpty()
        
        // Then
        coVerify { dataSource.getCurrencyList() }
        coVerify { assetDao.getEnabledAssetCodes() }
    }
    
    @Test
    fun `initializeIfEmpty should not refresh assets when database is not empty`() = runTest {
        // Given
        val existingAssets = listOf(
            AssetEntity("USD", "US Dollar", true, 1.0)
        )
        every { assetDao.getAllAssets() } returns flowOf(existingAssets)
        
        // When
        repository.initializeIfEmpty()
        
        // Then
        coVerify(exactly = 0) { dataSource.getCurrencyList() }
    }
    
    @Test
    fun `getMergedAssets should merge cached assets with API assets`() = runTest {
        // Given
        // Database has assets with enabled state
        val cachedAssets = listOf(
            AssetEntity("USD", "US Dollar", true, 1.0),
            AssetEntity("EUR", "Euro", true, 0.85)
        )
        // API returns all available assets
        val apiAssets = listOf(
            Asset("USD", "US Dollar", false),
            Asset("EUR", "Euro", false)
        )
        
        // Mock DB is not empty
        every { assetDao.getAllAssets() } returns flowOf(cachedAssets)
        // Mock API call
        coEvery { dataSource.getCurrencyList() } returns Result.success(
            CurrencyListResponse(
                success = true,
                currencies = mapOf("USD" to "US Dollar", "EUR" to "Euro")
            )
        )
        
        // When
        val result = repository.getMergedAssets()
        
        // Then
        assertEquals(2, result.size)
        // Verify cached assets preserve their enabled state
        assertEquals("USD", result[0].code)
        assertEquals(true, result[0].isEnabled)
        assertEquals("EUR", result[1].code)
        assertEquals(true, result[1].isEnabled)
        
        // Verify calls were made
        verify { assetDao.getAllAssets() }
        coVerify { dataSource.getCurrencyList() }
    }
} 