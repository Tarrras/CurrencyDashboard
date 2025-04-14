package com.example.currencydashboard.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencydashboard.R
import com.example.currencydashboard.domain.model.Asset
import com.example.currencydashboard.domain.usecase.GetAssetsUseCase
import com.example.currencydashboard.domain.usecase.ToggleAssetUseCase
import com.example.currencydashboard.presentation.common.UiError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddAssetViewModel(
    private val getAssetsUseCase: GetAssetsUseCase,
    private val toggleAssetUseCase: ToggleAssetUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(AddAssetState())
    val state: StateFlow<AddAssetState> = _state.asStateFlow()
    
    init {
        state
            .map { it.searchQuery }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                _state.update { it.copy(isLoading = true) }
                loadAssetsWithQuery(query)
            }
            .launchIn(viewModelScope)
    }
    
    fun handleIntent(intent: AddAssetIntent) {
        when (intent) {
            is AddAssetIntent.LoadAssets -> loadAssets()
            is AddAssetIntent.UpdateSearch -> updateSearch(intent.query)
            is AddAssetIntent.ToggleAsset -> toggleAsset(intent.code, intent.isEnabled)
        }
    }
    
    private fun loadAssets() {
        viewModelScope.launch {
            loadAssetsWithQuery(_state.value.searchQuery)
        }
    }
    
    private fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
    
    private suspend fun loadAssetsWithQuery(query: String) {
        try {
            val assets = getAssetsUseCase.getAllAssetsOnce(query)
            
            // Update state with search results
            _state.update { state ->
                state.copy(
                    assets = sortAssets(assets),
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoading = false,
                    error = e.message?.let { msg -> UiError.TextError(msg) } 
                        ?: UiError.ResourceError(R.string.failed_to_load_assets)
                )
            }
        }
    }
    
    private fun toggleAsset(code: String, isEnabled: Boolean) {
        viewModelScope.launch {
            toggleAssetUseCase(code, isEnabled)
                .onSuccess {
                    // Update local state immediately for UI responsiveness
                    _state.update { state ->
                        val updatedAssets = state.assets.map { asset ->
                            if (asset.code == code) {
                                asset.copy(isEnabled = isEnabled)
                            } else {
                                asset
                            }
                        }
                        
                        state.copy(
                            assets = sortAssets(updatedAssets),
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            error = error.message?.let { msg -> UiError.TextError(msg) } 
                                ?: UiError.ResourceError(R.string.failed_to_update_asset)
                        ) 
                    }
                }
        }
    }
    
    private fun sortAssets(assets: List<Asset>): List<Asset> {
        return assets.sortedWith(
            compareByDescending<Asset> { it.isEnabled }
                .thenBy { it.code }
        )
    }
}

data class AddAssetState(
    val assets: List<Asset> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: UiError? = null
)

sealed interface AddAssetIntent {
    data object LoadAssets : AddAssetIntent
    data class UpdateSearch(val query: String) : AddAssetIntent
    data class ToggleAsset(val code: String, val isEnabled: Boolean) : AddAssetIntent
} 