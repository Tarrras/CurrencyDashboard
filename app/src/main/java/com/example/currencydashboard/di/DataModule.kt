package com.example.currencydashboard.di

import android.content.Context
import androidx.room.Room
import com.example.currencydashboard.data.local.AssetDatabase
import com.example.currencydashboard.data.local.PreferencesManager
import com.example.currencydashboard.data.repository.AssetRepositoryImpl
import com.example.currencydashboard.domain.repository.AssetRepository
import com.example.currencydashboard.domain.usecase.GetAssetsUseCase
import com.example.currencydashboard.domain.usecase.GetRatesUseCase
import com.example.currencydashboard.domain.usecase.ToggleAssetUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val dataModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AssetDatabase::class.java,
            "assets_database"
        ).build()
    }
    
    // DAOs
    single { get<AssetDatabase>().assetDao() }
    
    // PreferencesManager
    single { PreferencesManager(androidContext()) }
    
    // Repositories
    single<AssetRepository> { AssetRepositoryImpl(get(), get(), get()) }
    
    // Use Cases
    factoryOf(::GetAssetsUseCase)
    factoryOf(::GetRatesUseCase)
    factoryOf(::ToggleAssetUseCase)
} 