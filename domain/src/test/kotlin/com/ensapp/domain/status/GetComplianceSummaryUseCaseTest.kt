package com.ensapp.domain.status

import com.ensapp.domain.catalog.ApplicabilityEvaluation
import com.ensapp.domain.catalog.ApplicabilityProvenance
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.response.MaturityLevel
import com.ensapp.domain.response.RequirementResponse
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class GetComplianceSummaryUseCaseTest {
    @Test
    fun `counts applicable answered pending and CIDAT dimensions without double counting`() =
        runBlocking {
            val repository = Mockito.mock(ComplianceStatusRepository::class.java)
            val records = listOf(
                record(
                    "r1",
                    ApplicabilityStatus.APPLIES,
                    setOf(SecurityDimension.CONFIDENCIALIDAD),
                    MaturityLevel.L0,
                ),
                record(
                    "r2",
                    ApplicabilityStatus.APPLIES,
                    setOf(
                        SecurityDimension.CONFIDENCIALIDAD,
                        SecurityDimension.INTEGRIDAD,
                    ),
                    null,
                ),
                record(
                    "r3",
                    ApplicabilityStatus.DOES_NOT_APPLY,
                    setOf(SecurityDimension.INTEGRIDAD),
                    MaturityLevel.L5,
                ),
                record("r4", ApplicabilityStatus.REVIEW, setOf(), null),
            )
            Mockito.`when`(repository.records("system-1")).thenReturn(records)

            val summary = GetComplianceSummaryUseCase(repository).execute("system-1")

            assertEquals(4, summary.totalRequirements)
            assertEquals(2, summary.applicable)
            assertEquals(1, summary.responded)
            assertEquals(1, summary.pending)
            assertEquals(50, summary.percentage)
            assertEquals(2, summary.byDimension.getValue(SecurityDimension.CONFIDENCIALIDAD).applicable)
            assertEquals(1, summary.byDimension.getValue(SecurityDimension.INTEGRIDAD).applicable)
            assertEquals(1, summary.byDimension.getValue(SecurityDimension.CONFIDENCIALIDAD).responded)
            assertEquals(0, summary.byDimension.getValue(SecurityDimension.INTEGRIDAD).responded)
            Mockito.verify(repository).records("system-1")
        }

    @Test
    fun `returns zero percentage when there are no applicable requirements`() = runBlocking {
        val repository = Mockito.mock(ComplianceStatusRepository::class.java)
        Mockito.`when`(repository.records("system-1")).thenReturn(
            listOf(record("r1", ApplicabilityStatus.DOES_NOT_APPLY, emptySet(), null)),
        )

        val summary = GetComplianceSummaryUseCase(repository).execute("system-1")

        assertEquals(0, summary.applicable)
        assertEquals(0, summary.percentage)
        assertEquals(0, summary.pending)
    }

    private fun record(
        id: String,
        status: ApplicabilityStatus,
        dimensions: Set<SecurityDimension>,
        maturity: MaturityLevel?,
    ) = RequirementComplianceRecord(
        requirementId = id,
        applicability = ApplicabilityEvaluation(
            status,
            null,
            ApplicabilityProvenance.OWN,
        ),
        dimensions = dimensions,
        response = maturity?.let {
            RequirementResponse("system-1", id, it, null, Instant.EPOCH)
        },
    )
}
