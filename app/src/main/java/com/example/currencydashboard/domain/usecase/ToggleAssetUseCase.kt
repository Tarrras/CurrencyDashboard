package com.example.currencydashboard.domain.usecase

import com.example.currencydashboard.domain.repository.AssetRepository

class ToggleAssetUseCase(private val repository: AssetRepository) {

    suspend operator fun invoke(code: String, isEnabled: Boolean): Result<Unit> {
        return repository.toggleAsset(code, isEnabled)
    }
} 