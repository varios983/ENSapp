package com.ensapp.presentation.system

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.system.InformationSystem

@Composable
fun SystemManagementScreen(
    viewModel: SystemViewModel,
    onOpenSystem: (InformationSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<InformationSystem?>(null) }

    LaunchedEffect(state.loading, state.systems.size) {
        if (!state.loading && state.systems.isEmpty()) creating = true
    }

    when {
        state.loading && state.systems.isEmpty() -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("Cargando sistemas")
            }
        }
        creating || editing != null -> {
            EditSystemScreen(
                initial = editing,
                saving = state.saving,
                error = state.error,
                onCancel = {
                    creating = false
                    editing = null
                },
                onSave = { name, category, dimensions ->
                    viewModel.save(editing, name, category, dimensions) {
                        creating = false
                        editing = null
                        onOpenSystem(it)
                    }
                },
                modifier = modifier,
            )
        }
        else -> {
            Column(
                modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text("Sistemas de Información", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { creating = true }) { Text("Crear sistema") }
                Spacer(Modifier.height(8.dp))
                state.error?.let { ErrorMessage(it) }
                state.systems.forEach { system ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(system.name, style = MaterialTheme.typography.titleMedium)
                            Text("Categoría ${system.category.name}")
                            Text(
                                "Dimensiones configuradas: ${system.dimensions.size}/5",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.select(system.id)
                                        onOpenSystem(system)
                                    },
                                ) { Text("Abrir") }
                                OutlinedButton(onClick = { editing = system }) { Text("Editar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSystemScreen(
    initial: InformationSystem?,
    saving: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSave: (String, EnsCategory, Map<SecurityDimension, DimensionLevel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial?.id) {
        mutableStateOf(initial?.category ?: EnsCategory.BASICA)
    }
    var dimensions by remember(initial?.id) {
        mutableStateOf(
            initial?.dimensions
                ?: SecurityDimension.entries.associateWith { DimensionLevel.BAJO },
        )
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (initial == null) "Nuevo sistema" else "Editar sistema",
            style = MaterialTheme.typography.headlineSmall,
        )
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre del sistema") },
            singleLine = true,
        )
        Text("Categoría ENS", style = MaterialTheme.typography.titleMedium)
        EnsCategory.entries.forEach { option ->
            Row(
                Modifier.fillMaxWidth().clickable { category = option },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = category == option, onClick = { category = option })
                Text(option.name)
            }
        }
        HorizontalDivider()
        Text("Nivel de las dimensiones CIDAT", style = MaterialTheme.typography.titleMedium)
        SecurityDimension.entries.forEach { dimension ->
            Text(dimension.name, style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DimensionLevel.entries.forEach { level ->
                    FilterChip(
                        selected = dimensions[dimension] == level,
                        onClick = { dimensions = dimensions + (dimension to level) },
                        label = { Text(level.name) },
                    )
                }
            }
        }
        if (initial != null) {
            Text(
                "Al cambiar la configuración, algunas respuestas existentes pueden dejar de aplicar.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        error?.let { ErrorMessage(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSave(name, category, dimensions) },
                enabled = !saving && name.isNotBlank() &&
                    dimensions.keys == SecurityDimension.entries.toSet(),
            ) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
            OutlinedButton(onClick = onCancel, enabled = !saving) { Text("Cancelar") }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error)
}
