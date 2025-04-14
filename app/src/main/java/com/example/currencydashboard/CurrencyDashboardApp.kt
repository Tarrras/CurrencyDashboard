package com.example.currencydashboard

import android.app.Application
import com.example.currencydashboard.di.viewModelModule
import com.example.currencydashboard.di.dataModule
import com.example.currencydashboard.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CurrencyDashboardApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@CurrencyDashboardApp)
            modules(listOf(viewModelModule, dataModule, networkModule))
        }
    }
} 