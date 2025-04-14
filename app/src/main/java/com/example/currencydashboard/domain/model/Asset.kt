package com.example.currencydashboard.domain.model

data class Asset(
    val code: String,
    val name: String,
    val isEnabled: Boolean = false,
    val rate: Double = 0.0,
    val change: Double = 0.0
) 