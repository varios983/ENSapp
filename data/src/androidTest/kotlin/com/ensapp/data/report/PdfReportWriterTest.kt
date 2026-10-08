package com.ensapp.data.report

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ensapp.domain.catalog.ApplicabilityEvaluation
import com.ensapp.domain.catalog.ApplicabilityProvenance
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogPartNode
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.Family
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.Requirement
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.report.ReportRequirement
import com.ensapp.domain.report.ReportSnapshot
import com.ensapp.domain.status.ComplianceSummary
import com.ensapp.domain.system.InformationSystem
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfReportWriterTest {
    @Test
    fun generates_all_catalog_rows_in_under_five_seconds() {
        val start = System.nanoTime()
        val pdf = PdfReportWriter().write(snapshot())
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000

        assertTrue("PDF output is empty", pdf.size > 1_000)
        assertTrue("Generation took ${elapsedMillis}ms", elapsedMillis < 5_000)
    }

    private fun snapshot(): ReportSnapshot {
        val now = Instant.EPOCH
        val system = InformationSystem(
            "system-1",
            "Sistema de prueba",
            EnsCategory.MEDIA,
            SecurityDimension.entries.associateWith { DimensionLevel.MEDIO },
            now,
            now,
        )
        val framework = Framework("op", "Operacional", null, 0, emptyList())
        val family = Family("op.acc", "op", "Acceso", null, 0, emptyList())
        val measure = CatalogControl(
            "op.acc.1",
            ControlKind.MEASURE,
            null,
            "op",
            "op.acc",
            "Medida de acceso",
            null,
            null,
            0,
            emptyList(),
            null,
        )
        val templatePart = CatalogPart(
            "r-0",
            measure.id,
            null,
            null,
            "item",
            null,
            "R0",
            "Descripción de requisito para comprobar paginación.",
            0,
            emptyList(),
        )
        val template = ReportRequirement(
            framework,
            family,
            measure,
            null,
            RequirementBrowseItem(
                Requirement("r-0", measure.id, "R0", templatePart.prose.orEmpty(), 0),
                ApplicabilityEvaluation(
                    ApplicabilityStatus.APPLIES,
                    "Aplica",
                    ApplicabilityProvenance.OWN,
                ),
                emptySet(),
                null,
                CatalogPartNode(templatePart, emptyList()),
            ),
        )
        val rows = (0 until 386).map { index ->
            template.copy(
                item = template.item.copy(
                    requirement = template.item.requirement.copy(
                        id = "r-$index",
                        label = "R$index",
                        sortOrder = index,
                    ),
                    content = CatalogPartNode(
                        templatePart.copy(id = "r-$index", sortOrder = index),
                        emptyList(),
                    ),
                ),
            )
        }
        return ReportSnapshot(
            system,
            CatalogRevision("rev10", "uuid", "10", "1.1.3", null, "checksum"),
            ComplianceSummary(386, 386, 0, 386, 0, emptyMap()),
            rows,
        )
    }
}
