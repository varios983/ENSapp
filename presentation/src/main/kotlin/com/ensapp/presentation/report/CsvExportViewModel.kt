package com.ensapp.presentation.report

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CsvExportState(
    val exporting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class CsvExportViewModel(
    private val createCsv: suspend (String) -> ByteArray,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CsvExportState())
    val state = mutableState.asStateFlow()

    fun export(systemId: String, uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            mutableState.value = CsvExportState(exporting = true)
            try {
                withContext(Dispatchers.IO) {
                    val bytes = createCsv(systemId)
                    val output = resolver.openOutputStream(uri)
                        ?: error("No se pudo abrir el destino seleccionado")
                    output.use { it.write(bytes) }
                }
                mutableState.value = CsvExportState(message = "CSV exportado correctamente")
            } catch (exception: Exception) {
                mutableState.value = CsvExportState(
                    error = exception.message ?: "No se pudo exportar el CSV",
                )
            }
        }
    }
}
