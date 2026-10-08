package com.ensapp.domain.status

import com.ensapp.domain.catalog.ApplicabilityEvaluation
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.response.RequirementResponse

data class RequirementComplianceRecord(
    val requirementId: String,
    val applicability: ApplicabilityEvaluation,
    val dimensions: Set<SecurityDimension>,
    val response: RequirementResponse?,
)

data class DimensionComplianceSummary(
    val applicable: Int,
    val responded: Int,
    val pending: Int,
    val percentage: Int,
)

data class ComplianceSummary(
    val totalRequirements: Int,
    val applicable: Int,
    val responded: Int,
    val pending: Int,
    val percentage: Int,
    val byDimension: Map<SecurityDimension, DimensionComplianceSummary>,
)

interface ComplianceStatusRepository {
    suspend fun records(systemId: String): List<RequirementComplianceRecord>
}
