package com.ensapp.domain.report

import com.ensapp.domain.catalog.ApplicabilityStatus

class CsvReportWriter {
    fun write(snapshot: ReportSnapshot): ByteArray {
        val rows = buildList {
            add(HEADER.joinToString(","))
            snapshot.requirements.forEach { row ->
                val response = row.item.response
                    ?.takeIf { it.systemId == snapshot.system.id }
                val applicability = when (row.item.applicability.status) {
                    ApplicabilityStatus.APPLIES -> "APLICA"
                    ApplicabilityStatus.DOES_NOT_APPLY -> "NO_APLICA"
                    ApplicabilityStatus.REVIEW -> "REVISAR"
                }
                add(
                    listOf(
                        snapshot.system.id,
                        snapshot.system.name,
                        snapshot.revision.version ?: snapshot.revision.revisionId,
                        row.framework.id,
                        row.framework.label ?: row.framework.title,
                        row.family?.id.orEmpty(),
                        row.family?.let { it.label ?: it.title }.orEmpty(),
                        row.measure.id,
                        row.measure.title,
                        row.reinforcement?.id.orEmpty(),
                        row.reinforcement?.title.orEmpty(),
                        row.item.requirement.id,
                        row.item.requirement.label.orEmpty(),
                        row.item.requirement.text,
                        applicability,
                        row.item.applicability.reason.orEmpty(),
                        response?.maturity?.name.orEmpty(),
                        response?.note.orEmpty(),
                        response?.updatedAt?.toString().orEmpty(),
                    ).map(::escape).joinToString(","),
                )
            }
        }
        return rows.joinToString("\r\n", postfix = "\r\n").toByteArray(Charsets.UTF_8)
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    companion object {
        val HEADER = listOf(
            "sistema_id",
            "sistema_nombre",
            "catalog_revision",
            "marco_id",
            "marco_nombre",
            "familia_id",
            "familia_nombre",
            "medida_id",
            "medida_titulo",
            "refuerzo_id",
            "refuerzo_titulo",
            "requisito_id",
            "requisito_label",
            "requisito_texto",
            "aplicabilidad",
            "aplicabilidad_motivo",
            "madurez",
            "nota",
            "actualizado_en",
        )
    }
}
