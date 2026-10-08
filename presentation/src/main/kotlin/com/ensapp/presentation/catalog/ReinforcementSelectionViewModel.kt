package com.ensapp.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensapp.domain.catalog.CatalogParameter
import com.ensapp.domain.catalog.DisjunctiveSelectionRepository
import com.ensapp.domain.catalog.SelectReinforcementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReinforcementSelectionUiState(
    val selections: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
)

class ReinforcementSelectionViewModel(
    private val repository: DisjunctiveSelectionRepository,
    private val selectReinforcement: SelectReinforcementUseCase =
        SelectReinforcementUseCase(repository),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReinforcementSelectionUiState())
    val state: StateFlow<ReinforcementSelectionUiState> = mutableState.asStateFlow()

    fun load(systemId: String, parameters: List<CatalogParameter>) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val selections = parameters.mapNotNull { parameter ->
                    repository.selected(systemId, parameter.id)?.let {
                        parameter.id to it.reinforcementId
                    }
                }.toMap()
                mutableState.value = ReinforcementSelectionUiState(selections = selections)
            } catch (exception: Exception) {
                mutableState.value = ReinforcementSelectionUiState(
                    error = exception.message ?: "No se pudieron cargar las selecciones",
                )
            }
        }
    }

    fun select(systemId: String, parameter: CatalogParameter, reinforcementId: String) {
        viewModelScope.launch {
            try {
                val selection = selectReinforcement.execute(systemId, parameter, reinforcementId)
                mutableState.value = mutableState.value.copy(
                    selections = mutableState.value.selections +
                        (parameter.id to selection.reinforcementId),
                    error = null,
                )
            } catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(
                    error = exception.message ?: "No se pudo cambiar la selección",
                )
            }
        }
    }
}
