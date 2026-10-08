package com.ensapp.presentation.response

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensapp.domain.response.MaturityLevel
import com.ensapp.domain.response.RequirementResponse
import com.ensapp.domain.response.RequirementResponseRepository
import com.ensapp.domain.response.SaveRequirementResponseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RequirementResponseUiState(
    val response: RequirementResponse? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

class RequirementResponseViewModel(
    private val repository: RequirementResponseRepository,
    private val saveResponse: SaveRequirementResponseUseCase =
        SaveRequirementResponseUseCase(repository),
) : ViewModel() {
    private val mutableState = MutableStateFlow(RequirementResponseUiState())
    val state: StateFlow<RequirementResponseUiState> = mutableState.asStateFlow()

    fun load(systemId: String, requirementId: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                mutableState.value = RequirementResponseUiState(
                    response = repository.get(systemId, requirementId),
                )
            } catch (exception: Exception) {
                mutableState.value = RequirementResponseUiState(
                    error = exception.message ?: "No se pudo cargar la respuesta",
                )
            }
        }
    }

    fun save(
        systemId: String,
        requirementId: String,
        maturity: MaturityLevel,
        note: String?,
    ) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null)
            try {
                val response = saveResponse.execute(systemId, requirementId, maturity, note)
                mutableState.value = RequirementResponseUiState(response = response)
            } catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(
                    saving = false,
                    error = exception.message ?: "No se pudo guardar la respuesta",
                )
            }
        }
    }
}
