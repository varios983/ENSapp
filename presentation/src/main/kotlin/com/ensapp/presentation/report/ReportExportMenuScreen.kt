package com.ensapp.presentation.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ensapp.domain.system.InformationSystem

enum class ReportExportFormat {
    PDF,
    CSV,
}

@Composable
fun ReportExportMenuScreen(
    system: InformationSystem,
    onChoose: (ReportExportFormat) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Exportar evaluación", style = MaterialTheme.typography.headlineSmall)
        Text(system.name, style = MaterialTheme.typography.titleMedium)
        Button(onClick = { onChoose(ReportExportFormat.PDF) }) { Text("Exportar PDF") }
        Button(onClick = { onChoose(ReportExportFormat.CSV) }) { Text("Exportar CSV") }
        OutlinedButton(onClick = onBack) { Text("Volver al catálogo") }
    }
}
