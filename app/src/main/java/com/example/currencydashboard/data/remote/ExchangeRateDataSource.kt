package com.example.currencydashboard.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DataSource class that abstracts network operations and wraps API responses in Result
 */
class ExchangeRateDataSource(private val api: ExchangeRateApi) {

    suspend fun getLiveRates(source: String = "USD"): Result<LiveRatesResponse> = withContext(Dispatchers.IO) {
        runCatching {
            api.getLiveRates(source)
        }
    }

    suspend fun getCurrencyList(searchQuery: String = ""): Result<CurrencyListResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getCurrencyList()
            
            // If search query is empty, return the full response
            if (searchQuery.isBlank()) {
                return@runCatching response
            }
            
            // Otherwise, filter currencies locally by code or name
            val filteredCurrencies = response.currencies.filter { (code, name) ->
                code.contains(searchQuery, ignoreCase = true) || 
                name.contains(searchQuery, ignoreCase = true)
            }
            
            // Return a new response with filtered currencies
            CurrencyListResponse(
                success = response.success,
                currencies = filteredCurrencies
            )
        }
    }
} 