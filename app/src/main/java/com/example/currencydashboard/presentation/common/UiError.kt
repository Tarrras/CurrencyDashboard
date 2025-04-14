package com.example.currencydashboard.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiError {
    data class ResourceError(@StringRes val res: Int) : UiError()
    data class TextError(val error: String) : UiError()
}

@Composable
fun UiError.toUiText(): String {
    return when(this) {
        is UiError.ResourceError -> stringResource(this.res)
        is UiError.TextError -> this.error
    }
} 