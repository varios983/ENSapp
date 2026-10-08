package com.ensapp.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensapp.domain.catalog.BrowseCatalogUseCases
import com.ensapp.domain.catalog.CatalogBrowseTree
import com.ensapp.domain.system.InformationSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val tree: CatalogBrowseTree? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class CatalogViewModel(
    private val browseCatalog: BrowseCatalogUseCases,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = mutableState.asStateFlow()

    fun load(system: InformationSystem) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val tree = browseCatalog.execute(system)
                mutableState.value = CatalogUiState(tree = tree)
            } catch (exception: Exception) {
                mutableState.value = CatalogUiState(
                    error = exception.message ?: "No se pudo consultar el catálogo",
                )
            }
        }
    }
}
