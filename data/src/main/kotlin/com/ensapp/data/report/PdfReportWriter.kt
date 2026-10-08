package com.ensapp.data.report

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.report.ReportSnapshot
import java.io.ByteArrayOutputStream

class PdfReportWriter {
    fun write(snapshot: ReportSnapshot): ByteArray {
        val document = PdfDocument()
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            isFakeBoldText = true
        }
        try {
        val lines = buildList {
            add("Evaluación ENS - ${snapshot.system.name}")
            add("Sistema: ${snapshot.system.id}")
            add("Catálogo: ${snapshot.revision.version ?: snapshot.revision.revisionId}")
            add("Categoría: ${snapshot.system.category}")
            snapshot.system.dimensions.forEach { (dimension, level) ->
                add("$dimension: $level")
            }
            add(
                "Resumen: ${snapshot.summary.responded}/${snapshot.summary.applicable} " +
                    "requisitos aplicables respondidos (${snapshot.summary.percentage}%)",
            )
            add("Pendientes: ${snapshot.summary.pending}; total: ${snapshot.summary.totalRequirements}")
            add("")
            snapshot.requirements.forEach { row ->
                val status = when (row.item.applicability.status) {
                    ApplicabilityStatus.APPLIES -> "APLICA"
                    ApplicabilityStatus.DOES_NOT_APPLY -> "NO APLICA"
                    ApplicabilityStatus.REVIEW -> "REVISAR"
                }
                add("${row.framework.label ?: row.framework.title} (${row.framework.id})")
                row.family?.let { add("  ${it.label ?: it.title} (${it.id})") }
                add("  Medida: ${row.measure.id} - ${row.measure.title}")
                row.reinforcement?.let { add("    Refuerzo: ${it.id} - ${it.title}") }
                add(
                    "      Requisito ${row.item.requirement.id} " +
                        "(${row.item.requirement.label.orEmpty()}): " +
                        row.item.requirement.text,
                )
                add("        Aplicabilidad: $status. ${row.item.applicability.reason.orEmpty()}")
                val response = row.item.response?.takeIf { it.systemId == snapshot.system.id }
                add("        Madurez: ${response?.maturity?.name ?: "Sin responder"}")
                response?.note?.takeIf { it.isNotBlank() }?.let { add("        Nota: $it") }
            }
        }
        val wrapped = lines.flatMap { wrap(it, bodyPaint, CONTENT_WIDTH) }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = TOP
        wrapped.forEachIndexed { index, line ->
            if (y + LINE_HEIGHT > PAGE_HEIGHT - BOTTOM) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
                )
                canvas = page.canvas
                y = TOP
            }
            val paint = if (index == 0) headingPaint else bodyPaint
            canvas.drawText(line, LEFT, y, paint)
            y += LINE_HEIGHT
        }
        document.finishPage(page)
        return ByteArrayOutputStream().use { output ->
            document.writeTo(output)
            output.toByteArray()
        }
        } finally {
            document.close()
        }
    }

    private fun wrap(text: String, paint: Paint, width: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(WHITESPACE)
        val result = mutableListOf<String>()
        var line = StringBuilder()
        words.forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= width) {
                line = StringBuilder(candidate)
            } else {
                if (line.isNotEmpty()) result += line.toString()
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) result += line.toString()
        return result
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val LEFT = 36f
        const val TOP = 48f
        const val BOTTOM = 42f
        const val LINE_HEIGHT = 14f
        const val CONTENT_WIDTH = PAGE_WIDTH - LEFT * 2
        val WHITESPACE = Regex("\\s+")
    }
}
