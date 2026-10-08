package com.ensapp.domain.report

import com.ensapp.domain.catalog.ApplicabilityRule
import com.ensapp.domain.catalog.BrowseCatalogUseCases
import com.ensapp.domain.catalog.Catalog
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogRepository
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.Family
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.Requirement
import com.ensapp.domain.response.RequirementResponse
import com.ensapp.domain.response.RequirementResponseRepository
import com.ensapp.domain.status.ComplianceStatusRepository
import com.ensapp.domain.status.GetComplianceSummaryUseCase
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.system.InformationSystem
import com.ensapp.domain.system.InformationSystemRepository
import com.ensapp.domain.catalog.SecurityDimension
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class BuildReportSnapshotUseCaseTest {
    @Test
    fun `builds selected system ordered hierarchy summary and excludes foreign responses`() =
        runBlocking {
            val systemRepository = Mockito.mock(InformationSystemRepository::class.java)
            val catalogRepository = Mockito.mock(CatalogRepository::class.java)
            val responseRepository = Mockito.mock(RequirementResponseRepository::class.java)
            val statusRepository = Mockito.mock(ComplianceStatusRepository::class.java)
            val system = system()
            Mockito.`when`(systemRepository.get("sys-1")).thenReturn(system)
            Mockito.`when`(catalogRepository.activeCatalog()).thenReturn(catalog())
            Mockito.`when`(responseRepository.list("sys-1")).thenReturn(
                listOf(
                    RequirementResponse(
                        "another-system",
                        "r1",
                        com.ensapp.domain.response.MaturityLevel.L0,
                        null,
                        Instant.EPOCH,
                    ),
                    RequirementResponse(
                        "sys-1",
                        "r2",
                        com.ensapp.domain.response.MaturityLevel.L0,
                        "Nota",
                        Instant.EPOCH,
                    ),
                ),
            )
            val snapshotUseCase = BuildReportSnapshotUseCase(
                systemRepository,
                BrowseCatalogUseCases(catalogRepository, responseRepository = responseRepository),
                GetComplianceSummaryUseCase(statusRepository),
            )

            val snapshot = snapshotUseCase.execute("sys-1")

            assertEquals("sys-1", snapshot.system.id)
            assertEquals("rev-10", snapshot.revision.revisionId)
            assertEquals(386, snapshot.summary.totalRequirements)
            assertEquals(1, snapshot.summary.responded)
            assertEquals(386, snapshot.requirements.size)
            assertEquals(listOf("r1", "r2"), snapshot.requirements.take(2).map { it.item.requirement.id })
            assertEquals("op.acc.1", snapshot.requirements[0].measure.id)
            assertEquals("op.acc.1.r1", snapshot.requirements[1].reinforcement?.id)
            assertNull(snapshot.requirements.first().item.response)
            assertEquals(
                com.ensapp.domain.response.MaturityLevel.L0,
                snapshot.requirements[1].item.response?.maturity,
            )
            Mockito.verifyNoInteractions(statusRepository)
        }

    private fun system() = InformationSystem(
        "sys-1",
        "Sistema",
        EnsCategory.MEDIA,
        SecurityDimension.entries.associateWith { DimensionLevel.MEDIO },
        Instant.EPOCH,
        Instant.EPOCH,
    )

    private fun catalog(): Catalog {
        val framework = Framework("op", "Operacional", "OP", 0, emptyList())
        val family = Family("op.acc", "op", "Acceso", "acc", 0, emptyList())
        val measure = CatalogControl(
            "op.acc.1",
            ControlKind.MEASURE,
            null,
            "op",
            "op.acc",
            "Medida",
            null,
            "M1",
            0,
            emptyList(),
            ApplicabilityRule(
                com.ensapp.domain.catalog.ApplicabilityCriterion.CATEGORY,
                categories = setOf(EnsCategory.MEDIA),
            ),
        )
        val reinforcement = CatalogControl(
            "op.acc.1.r1",
            ControlKind.REINFORCEMENT,
            measure.id,
            "op",
            "op.acc",
            "Refuerzo",
            "ens-refuerzo",
            null,
            0,
            emptyList(),
            ApplicabilityRule(
                com.ensapp.domain.catalog.ApplicabilityCriterion.CATEGORY,
                categories = setOf(EnsCategory.MEDIA),
            ),
        )
        val requirements = buildList {
            add(Requirement("r1", measure.id, "R1", "Primero", 0))
            (2..386).forEach { index ->
                add(
                    Requirement(
                        "r$index",
                        reinforcement.id,
                        "R$index",
                        "Requisito $index",
                        index - 2,
                    ),
                )
            }
        }
        val parts = requirements.mapIndexed { index, requirement ->
            CatalogPart(
                requirement.id,
                requirement.controlId,
                null,
                null,
                "item",
                null,
                requirement.label,
                requirement.text,
                index,
                emptyList(),
            )
        }
        return Catalog(
            CatalogRevision("rev-10", "uuid", "10", "1.1.3", null, "checksum"),
            listOf(framework),
            listOf(family),
            listOf(measure, reinforcement),
            emptyList(),
            parts,
            requirements,
        )
    }
}
