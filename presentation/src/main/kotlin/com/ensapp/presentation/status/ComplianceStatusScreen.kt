package com.ensapp.presentation.status

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensapp.domain.catalog.RequirementFilterStatus
import com.ensapp.domain.catalog.RequirementListFilter
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.system.InformationSystem

@Composable
fun ComplianceStatusScreen(
    viewModel: ComplianceViewModel,
    system: InformationSystem,
    onBack: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenRequirements: (RequirementListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(system.id, system.updatedAt) { viewModel.load(system.id) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onBack) { Text("Volver al catálogo") }
            Button(onClick = onOpenExport) { Text("Exportar") }
        }
        Text("Estado de cumplimiento", style = MaterialTheme.typography.headlineSmall)
        Text(system.name, style = MaterialTheme.typography.titleMedium)
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
            state.summary != null -> {
                val summary = state.summary!!
                Text(
                    "${summary.responded}/${summary.applicable} requisitos aplicables respondidos " +
                        "(${summary.percentage}%)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onOpenRequirements(RequirementListFilter(RequirementFilterStatus.ANSWERED))
                        },
                    ) { Text("Respondidos: ${summary.responded}") }
                    OutlinedButton(
                        onClick = {
                            onOpenRequirements(RequirementListFilter(RequirementFilterStatus.PENDING))
                        },
                    ) { Text("Pendientes: ${summary.pending}") }
                }
                Text("Total de requisitos en el catálogo: ${summary.totalRequirements}")
                Spacer(Modifier.height(8.dp))
                SecurityDimension.entries.forEach { dimension ->
                    val item = summary.byDimension.getValue(dimension)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(dimension.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.responded}/${item.applicable} respondidos " +
                                    "(${item.percentage}%) · ${item.pending} pendientes",
                            )
                            TextButton(
                                onClick = {
                                    onOpenRequirements(
                                        RequirementListFilter(
                                            RequirementFilterStatus.PENDING,
                                            dimension,
                                        ),
                                    )
                                },
                            ) { Text("Abrir pendientes de $dimension") }
                        }
                    }
                }
            }
        }
    }
}
