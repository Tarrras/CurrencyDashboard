package com.example.currencydashboard.data.remote

import com.google.gson.annotations.SerializedName

data class LiveRatesResponse(
    val success: Boolean,
    val timestamp: Long,
    val source: String,
    val quotes: Map<String, Double>
)

data class CurrencyListResponse(
    val success: Boolean,
    val currencies: Map<String, String>
) 