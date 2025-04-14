package com.example.currencydashboard.di

import com.example.currencydashboard.presentation.add.AddAssetViewModel
import com.example.currencydashboard.presentation.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddAssetViewModel)
}