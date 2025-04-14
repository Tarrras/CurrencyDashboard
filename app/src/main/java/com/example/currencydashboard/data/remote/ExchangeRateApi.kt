package com.example.currencydashboard.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
    @GET("live")
    suspend fun getLiveRates(@Query("source") source: String = "USD"): LiveRatesResponse
    
    @GET("list")
    suspend fun getCurrencyList(): CurrencyListResponse
} 