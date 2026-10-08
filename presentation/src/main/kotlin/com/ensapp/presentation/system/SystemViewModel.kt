package com.ensapp.presentation.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.system.ConfigureSystemUseCase
import com.ensapp.domain.system.InformationSystem
import com.ensapp.domain.system.InformationSystemRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SystemUiState(
    val systems: List<InformationSystem> = emptyList(),
    val selectedSystemId: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
)

class SystemViewModel(
    private val repository: InformationSystemRepository,
    private val configureSystem: ConfigureSystemUseCase = ConfigureSystemUseCase(repository),
) : ViewModel() {
    private val mutableState = MutableStateFlow(SystemUiState())
    val state: StateFlow<SystemUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val systems = repository.list()
                mutableState.value = mutableState.value.copy(
                    systems = systems,
                    selectedSystemId = mutableState.value.selectedSystemId
                        ?.takeIf { id -> systems.any { it.id == id } },
                    loading = false,
                )
            } catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    error = exception.message ?: "No se pudieron cargar los sistemas",
                )
            }
        }
    }

    fun select(systemId: String) {
        mutableState.value = mutableState.value.copy(selectedSystemId = systemId, error = null)
    }

    fun save(
        existing: InformationSystem?,
        name: String,
        category: EnsCategory,
        dimensions: Map<SecurityDimension, DimensionLevel>,
        onSaved: (InformationSystem) -> Unit,
    ) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null)
            try {
                val now = Instant.now()
                val system = InformationSystem(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    category = category,
                    dimensions = dimensions,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
                configureSystem.execute(system)
                val systems = repository.list()
                mutableState.value = mutableState.value.copy(
                    systems = systems,
                    selectedSystemId = system.id,
                    saving = false,
                )
                onSaved(system)
            } catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(
                    saving = false,
                    error = exception.message ?: "No se pudo guardar el sistema",
                )
            }
        }
    }
}
