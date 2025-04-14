package com.example.currencydashboard.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencydashboard.R
import com.example.currencydashboard.domain.model.Asset
import com.example.currencydashboard.domain.usecase.GetRatesUseCase
import com.example.currencydashboard.presentation.common.UiError
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class HomeViewModel(
    private val getRatesUseCase: GetRatesUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val refreshInterval = 3.seconds

    val assetsFlow = getRatesUseCase.getEnabledAssetsFlow().onEach { assets ->
        _state.update { it.copy(assets = assets) }
    }.launchIn(viewModelScope)

    val lastUpdatedFlow = getRatesUseCase.getLastRefreshTimestampFlow().onEach { timestamp ->
        _state.update { it.copy(lastUpdated = timestamp) }
    }.launchIn(viewModelScope)

    // Auto-refresh flow that triggers refresh at regular intervals
    private val autoRefreshFlow = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(refreshInterval)
        }
    }.onEach {
        refreshRates()
    }.launchIn(viewModelScope)
    
    init {
        viewModelScope.launch {
            // Initialize database if needed
            getRatesUseCase.initializeIfEmpty()
            
            // Set the current base currency in the state
            _state.update { it.copy(baseCurrency = getRatesUseCase.getBaseCurrency()) }

            // Initial refresh
            refreshRates()
        }
    }
    
    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.RefreshRates -> refreshRates()
        }
    }
    
    private fun refreshRates() {
        if (_state.value.isLoading) return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getRatesUseCase.refreshRates()
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        error = null
                    ) }
                    // Last updated timestamp is updated via Flow from PreferencesManager
                }
                .onFailure { error ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = error.message?.let { msg -> UiError.TextError(msg) } 
                            ?: UiError.ResourceError(R.string.failed_to_refresh_rates)
                    ) }
                }
        }
    }
}

data class HomeState(
    val assets: List<Asset> = emptyList(),
    val isLoading: Boolean = false,
    val lastUpdated: Long = 0L,
    val error: UiError? = null,
    val baseCurrency: String = "USD"  // Default base currency
)

sealed interface HomeIntent {
    data object RefreshRates : HomeIntent
}
