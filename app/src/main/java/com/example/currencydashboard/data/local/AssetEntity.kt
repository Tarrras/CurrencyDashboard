package com.example.currencydashboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.currencydashboard.domain.model.Asset

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val code: String,
    val name: String,
    val isEnabled: Boolean = false,
    val rate: Double = 0.0,
    val change: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun AssetEntity.toDomain(): Asset {
    return Asset(
        code = code,
        name = name,
        isEnabled = isEnabled,
        rate = rate,
        change = change
    )
}

fun Asset.toEntity(): AssetEntity {
    return AssetEntity(
        code = code,
        name = name,
        isEnabled = isEnabled,
        rate = rate,
        change = change
    )
} 