package com.ensapp.presentation.catalog

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensapp.domain.catalog.CatalogParameter

@Composable
fun ReinforcementSelection(
    viewModel: ReinforcementSelectionViewModel,
    systemId: String,
    parameters: List<CatalogParameter>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(systemId, parameters) {
        viewModel.load(systemId, parameters)
    }
    Column(modifier) {
        parameters
            .filter { it.type == "seleccion-refuerzo" }
            .forEach { parameter ->
                Text(
                    parameter.label ?: parameter.id,
                    style = MaterialTheme.typography.titleSmall,
                )
                parameter.usage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                parameter.choices.forEach { choice ->
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = state.selections[parameter.id] == choice,
                            enabled = enabled,
                            role = Role.RadioButton,
                        ) {
                                viewModel.select(systemId, parameter, choice)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.selections[parameter.id] == choice,
                            onClick = null,
                            enabled = enabled,
                        )
                        Text(choice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        if (!enabled && parameters.isNotEmpty()) {
            Text("Las alternativas están bloqueadas porque el control no aplica.")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
