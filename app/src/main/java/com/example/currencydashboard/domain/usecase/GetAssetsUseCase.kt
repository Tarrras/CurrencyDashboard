package com.example.currencydashboard.domain.usecase

import com.example.currencydashboard.domain.repository.AssetRepository
import com.example.currencydashboard.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetAssetsUseCase(private val assetRepository: AssetRepository) {
    operator fun invoke(): Flow<List<Asset>> {
        return assetRepository.getAllAssets()
    }

    suspend fun getAllAssetsOnce(searchQuery: String = ""): List<Asset> {
        // Delegate to repository's implementation of merging assets
        return assetRepository.getMergedAssets(searchQuery)
    }

    suspend fun initializeIfEmpty() {
        assetRepository.initializeIfEmpty()
    }
} 