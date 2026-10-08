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

data class PdfExportState(
    val exporting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class PdfExportViewModel(
    private val createPdf: suspend (String) -> ByteArray,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PdfExportState())
    val state = mutableState.asStateFlow()

    fun export(systemId: String, uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            mutableState.value = PdfExportState(exporting = true)
            try {
                withContext(Dispatchers.IO) {
                    val bytes = createPdf(systemId)
                    val output = resolver.openOutputStream(uri)
                        ?: error("No se pudo abrir el destino seleccionado")
                    output.use { it.write(bytes) }
                }
                mutableState.value = PdfExportState(message = "PDF exportado correctamente")
            } catch (exception: Exception) {
                mutableState.value = PdfExportState(
                    error = exception.message ?: "No se pudo exportar el PDF",
                )
            }
        }
    }
}
