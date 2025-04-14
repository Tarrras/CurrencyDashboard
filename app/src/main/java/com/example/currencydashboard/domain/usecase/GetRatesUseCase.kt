package com.example.currencydashboard.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.example.currencydashboard.domain.model.Asset
import com.example.currencydashboard.domain.repository.AssetRepository

class GetRatesUseCase(private val repository: AssetRepository) {

    suspend fun refreshRates(): Result<Unit> {
        return repository.refreshRates()
    }

    fun getEnabledAssetsFlow(): Flow<List<Asset>> {
        return repository.getEnabledAssets()
    }

    fun getLastRefreshTimestampFlow(): Flow<Long> {
        return repository.getLastRefreshTimestampFlow()
    }

    suspend fun initializeIfEmpty() {
        repository.initializeIfEmpty()
    }

    fun getBaseCurrency(): String {
        return repository.getBaseCurrency()
    }
} 