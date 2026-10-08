package com.ensapp.presentation.response

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensapp.domain.catalog.CatalogPartNode
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.response.MaturityLevel
import com.ensapp.domain.system.InformationSystem

@Composable
fun RequirementResponseScreen(
    viewModel: RequirementResponseViewModel,
    system: InformationSystem,
    item: RequirementBrowseItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var maturity by remember(system.id, item.requirement.id, state.response?.updatedAt) {
        mutableStateOf(state.response?.maturity)
    }
    var note by remember(system.id, item.requirement.id, state.response?.updatedAt) {
        mutableStateOf(state.response?.note.orEmpty())
    }

    LaunchedEffect(system.id, item.requirement.id) {
        viewModel.load(system.id, item.requirement.id)
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text("Atrás al catálogo") }
        Text("Sistema: ${system.name}", style = MaterialTheme.typography.titleMedium)
        Text(
            item.requirement.label ?: item.requirement.id,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(item.requirement.text)
        item.content.children.forEach { PartContent(it) }
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
            state.response == null -> Text(
                "Sin responder",
                style = MaterialTheme.typography.titleMedium,
            )
            else -> Text(
                "Respuesta guardada: ${state.response?.maturity?.name}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text("Nivel de madurez", style = MaterialTheme.typography.titleMedium)
        MaturityLevel.entries.forEach { option ->
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = maturity == option,
                    role = Role.RadioButton,
                ) { maturity = option },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = maturity == option,
                    onClick = null,
                )
                Text("${option.name} · ${maturityDescription(option)}")
            }
        }
        TextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nota de evidencia (opcional)") },
            minLines = 3,
        )
        Button(
            onClick = {
                maturity?.let { viewModel.save(system.id, item.requirement.id, it, note) }
            },
            enabled = maturity != null && !state.saving &&
                item.applicability.status == ApplicabilityStatus.APPLIES,
        ) {
            Text(if (state.saving) "Guardando…" else "Guardar respuesta")
        }
    }
}

@Composable
private fun PartContent(node: CatalogPartNode) {
    Column(Modifier.padding(start = 12.dp)) {
        node.part.prose?.let { Text(it) }
        node.children.forEach { PartContent(it) }
    }
}

private fun maturityDescription(level: MaturityLevel): String =
    when (level) {
        MaturityLevel.L0 -> "Inexistente"
        MaturityLevel.L1 -> "Inicial"
        MaturityLevel.L2 -> "Reproducible"
        MaturityLevel.L3 -> "Proceso definido"
        MaturityLevel.L4 -> "Gestionado y medible"
        MaturityLevel.L5 -> "Optimizado"
    }
