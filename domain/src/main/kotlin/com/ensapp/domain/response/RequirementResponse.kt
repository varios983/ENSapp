package com.ensapp.domain.response

import java.time.Instant

enum class MaturityLevel {
    L0,
    L1,
    L2,
    L3,
    L4,
    L5,
}

data class RequirementResponse(
    val systemId: String,
    val requirementId: String,
    val maturity: MaturityLevel,
    val note: String?,
    val updatedAt: Instant,
)

interface RequirementResponseRepository {
    suspend fun get(systemId: String, requirementId: String): RequirementResponse?
    suspend fun list(systemId: String): List<RequirementResponse>
    suspend fun save(response: RequirementResponse)
    suspend fun isRespondibleAndApplicable(systemId: String, requirementId: String): Boolean
}
