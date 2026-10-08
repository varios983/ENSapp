package com.ensapp.domain.report

import com.ensapp.domain.catalog.ApplicabilityEvaluation
import com.ensapp.domain.catalog.ApplicabilityProvenance
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogPartNode
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.Requirement
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.response.MaturityLevel
import com.ensapp.domain.response.RequirementResponse
import com.ensapp.domain.status.ComplianceSummary
import com.ensapp.domain.system.InformationSystem
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CsvReportWriterTest {
    @Test
    fun `writes UTF8 contract rows escaped and differentiates empty from L0`() {
        val system = InformationSystem(
            id = "sys-1",
            name = "Sistema, \"Águila\"",
            category = EnsCategory.MEDIA,
            dimensions = SecurityDimension.entries.associateWith { DimensionLevel.MEDIO },
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val framework = Framework("op", "Operacional", null, 0, emptyList())
        val measure = CatalogControl(
            "op.acc.1",
            ControlKind.MEASURE,
            null,
            "op",
            "op.acc",
            "Acceso",
            null,
            null,
            0,
            emptyList(),
            null,
        )
        val rootPart = CatalogPart("r1", "op.acc.1", null, null, "item", null, "R1", null, 0, emptyList())
        val applicability = ApplicabilityEvaluation(
            ApplicabilityStatus.DOES_NOT_APPLY,
            "No aplica, motivo",
            ApplicabilityProvenance.OWN,
        )
        val first = RequirementBrowseItem(
            Requirement("r1", "op.acc.1", "R1", "Texto con coma, comillas \"y\" salto\nlínea", 0),
            applicability,
            emptySet(),
            null,
            CatalogPartNode(rootPart, emptyList()),
        )
        val second = first.copy(
            requirement = first.requirement.copy(id = "r2", sortOrder = 1),
            applicability = applicability.copy(status = ApplicabilityStatus.APPLIES),
            response = RequirementResponse("sys-1", "r2", MaturityLevel.L0, "evidencia", Instant.EPOCH),
        )
        val snapshot = ReportSnapshot(
            system,
            CatalogRevision("rev10", "uuid", "10", "1.1.3", null, "hash"),
            ComplianceSummary(2, 1, 1, 0, 100, emptyMap()),
            listOf(
                ReportRequirement(framework, null, measure, null, first),
                ReportRequirement(framework, null, measure, null, second),
                ReportRequirement(
                    framework,
                    null,
                    measure,
                    null,
                    first.copy(
                        requirement = first.requirement.copy(id = "r3"),
                        applicability = applicability.copy(
                            status = ApplicabilityStatus.REVIEW,
                            reason = "Regla pendiente",
                        ),
                    ),
                ),
            ),
        )

        val csv = CsvReportWriter().write(snapshot).toString(Charsets.UTF_8)
        val lines = csv.split("\r\n")

        assertEquals(CsvReportWriter.HEADER.joinToString(","), lines.first())
        assertTrue(lines.size >= 4)
        assertTrue(csv.contains("\"Sistema, \"\"Águila\"\"\""))
        assertTrue(csv.contains("\"Texto con coma, comillas \"\"y\"\" salto\nlínea\""))
        assertTrue(lines[1].contains(",NO_APLICA,"))
        assertTrue(lines[1].endsWith(",,,"))
        assertTrue(
            csv.contains(",APLICA,\"No aplica, motivo\",L0,evidencia,1970-01-01T00:00:00Z"),
            csv,
        )
        assertTrue(csv.contains(",REVISAR,Regla pendiente,,,"))
        assertTrue(csv.contains("Águila"))
    }

    @Test
    fun `emits one CSV record for every requirement in the snapshot`() {
        val base = snapshot()
        val row = base.requirements.first()
        val fullSnapshot = base.copy(
            requirements = (0 until 386).map { index ->
                row.copy(
                    item = row.item.copy(
                        requirement = row.item.requirement.copy(
                            id = "req-$index",
                            sortOrder = index,
                        ),
                    ),
                )
            },
        )

        val records = CsvReportWriter().write(fullSnapshot)
            .toString(Charsets.UTF_8)
            .split("\r\n")
            .filter { it.isNotEmpty() }

        assertEquals(387, records.size)
        assertEquals("sistema_id", records.first().substringBefore(","))
    }

    private fun snapshot(): ReportSnapshot {
        val system = InformationSystem(
            id = "system",
            name = "System",
            category = EnsCategory.BASICA,
            dimensions = SecurityDimension.entries.associateWith { DimensionLevel.BAJO },
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val framework = Framework("org", "Organizativo", null, 0, emptyList())
        val measure = CatalogControl(
            "org.1",
            ControlKind.MEASURE,
            null,
            "org",
            null,
            "Medida",
            null,
            null,
            0,
            emptyList(),
            null,
        )
        val part = CatalogPart("req-1", measure.id, null, null, "item", null, null, "texto", 0, emptyList())
        val item = RequirementBrowseItem(
            Requirement("req-1", measure.id, null, "texto", 0),
            ApplicabilityEvaluation(
                ApplicabilityStatus.APPLIES,
                null,
                ApplicabilityProvenance.OWN,
            ),
            emptySet(),
            null,
            CatalogPartNode(part, emptyList()),
        )
        return ReportSnapshot(
            system,
            CatalogRevision("rev", "uuid", "1", "1.1.3", null, "checksum"),
            ComplianceSummary(1, 1, 0, 1, 0, emptyMap()),
            listOf(ReportRequirement(framework, null, measure, null, item)),
        )
    }
}
