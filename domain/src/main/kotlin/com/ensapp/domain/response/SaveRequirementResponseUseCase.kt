package com.ensapp.domain.response

import java.time.Instant

class SaveRequirementResponseUseCase(
    private val repository: RequirementResponseRepository,
) {
    suspend fun execute(
        systemId: String,
        requirementId: String,
        maturity: MaturityLevel,
        note: String?,
    ): RequirementResponse {
        require(systemId.isNotBlank()) { "Falta el sistema de información" }
        require(requirementId.isNotBlank()) { "Falta el requisito" }
        require(repository.isRespondibleAndApplicable(systemId, requirementId)) {
            "El requisito no es respondible o no aplica a este sistema"
        }
        val response = RequirementResponse(
            systemId = systemId,
            requirementId = requirementId,
            maturity = maturity,
            note = note,
            updatedAt = Instant.now(),
        )
        repository.save(response)
        return response
    }
}
