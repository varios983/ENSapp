package com.ensapp.presentation.report

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensapp.domain.system.InformationSystem

@Composable
fun PdfExportScreen(
    system: InformationSystem,
    viewModel: PdfExportViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) viewModel.export(system.id, uri, resolver)
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Exportar informe PDF", style = MaterialTheme.typography.headlineSmall)
        Text(system.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "Aviso de seguridad: el PDF exportado no queda cifrado por ENSapp. " +
                "Desde que se guarda, el usuario u organización es responsable de proteger " +
                "el archivo y elegir un destino adecuado.",
        )
        Button(
            onClick = { launcher.launch("evaluacion-ens-${system.name}.pdf") },
            enabled = !state.exporting,
        ) { Text("Elegir destino y exportar") }
        if (state.exporting) CircularProgressIndicator()
        state.message?.let { Text(it) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onBack) { Text("Volver") }
    }
}
