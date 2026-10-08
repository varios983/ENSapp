package com.ensapp.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensapp.domain.status.ComplianceStatusRepository
import com.ensapp.domain.status.ComplianceSummary
import com.ensapp.domain.status.GetComplianceSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComplianceUiState(
    val summary: ComplianceSummary? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class ComplianceViewModel(
    repository: ComplianceStatusRepository,
    private val getSummary: GetComplianceSummaryUseCase =
        GetComplianceSummaryUseCase(repository),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ComplianceUiState())
    val state: StateFlow<ComplianceUiState> = mutableState.asStateFlow()

    fun load(systemId: String) {
        viewModelScope.launch {
            mutableState.value = ComplianceUiState(loading = true)
            try {
                mutableState.value = ComplianceUiState(summary = getSummary.execute(systemId))
            } catch (exception: Exception) {
                mutableState.value = ComplianceUiState(
                    error = exception.message ?: "No se pudo calcular el estado",
                )
            }
        }
    }
}
