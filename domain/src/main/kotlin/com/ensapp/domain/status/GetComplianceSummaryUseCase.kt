package com.ensapp.domain.status

import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.SecurityDimension

class GetComplianceSummaryUseCase(
    private val repository: ComplianceStatusRepository,
) {
    suspend fun execute(systemId: String): ComplianceSummary {
        require(systemId.isNotBlank()) { "Falta el sistema de información" }
        val records = repository.records(systemId)
        return calculate(records)
    }

    fun calculate(records: List<RequirementComplianceRecord>): ComplianceSummary {
        val applicable = records.filter {
            it.applicability.status == ApplicabilityStatus.APPLIES
        }
        val responded = applicable.count { it.response != null }
        val byDimension = SecurityDimension.entries.associateWith { dimension ->
            val dimensionRecords = applicable.filter { dimension in it.dimensions }
            val dimensionResponded = dimensionRecords.count { it.response != null }
            DimensionComplianceSummary(
                applicable = dimensionRecords.size,
                responded = dimensionResponded,
                pending = dimensionRecords.size - dimensionResponded,
                percentage = percentage(dimensionResponded, dimensionRecords.size),
            )
        }
        return ComplianceSummary(
            totalRequirements = records.size,
            applicable = applicable.size,
            responded = responded,
            pending = applicable.size - responded,
            percentage = percentage(responded, applicable.size),
            byDimension = byDimension,
        )
    }

    private fun percentage(responded: Int, applicable: Int): Int =
        if (applicable == 0) 0 else responded * 100 / applicable
}
