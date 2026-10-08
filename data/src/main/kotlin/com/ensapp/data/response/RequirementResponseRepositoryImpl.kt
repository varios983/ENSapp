package com.ensapp.data.response

import com.ensapp.data.local.RequirementResponseDao
import com.ensapp.data.local.RequirementResponseEntity
import com.ensapp.data.security.FieldCipher
import com.ensapp.domain.catalog.ApplicabilityCalculator
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.CatalogRepository
import com.ensapp.domain.response.MaturityLevel
import com.ensapp.domain.response.RequirementResponse
import com.ensapp.domain.response.RequirementResponseRepository
import com.ensapp.domain.system.InformationSystemRepository
import java.time.Instant

class RequirementResponseRepositoryImpl(
    private val dao: RequirementResponseDao,
    private val catalogRepository: CatalogRepository,
    private val systemRepository: InformationSystemRepository,
    private val cipher: FieldCipher,
    private val applicabilityCalculator: ApplicabilityCalculator = ApplicabilityCalculator(),
) : RequirementResponseRepository {
    override suspend fun get(systemId: String, requirementId: String): RequirementResponse? =
        dao.response(systemId, requirementId)?.toDomain()

    override suspend fun list(systemId: String): List<RequirementResponse> =
        dao.responses(systemId).map { it.toDomain() }

    override suspend fun save(response: RequirementResponse) {
        dao.upsert(
            RequirementResponseEntity(
                systemId = response.systemId,
                requirementId = response.requirementId,
                maturityCiphertext = cipher.encrypt(
                    response.maturity.name,
                    purpose(response.systemId, response.requirementId, "maturity"),
                ),
                noteCiphertext = response.note?.let {
                    cipher.encrypt(it, purpose(response.systemId, response.requirementId, "note"))
                },
                updatedAt = response.updatedAt.toEpochMilli(),
            ),
        )
    }

    override suspend fun isRespondibleAndApplicable(
        systemId: String,
        requirementId: String,
    ): Boolean {
        val catalog = catalogRepository.activeCatalog()
        val requirement = catalog.requirements.firstOrNull { it.id == requirementId } ?: return false
        val system = systemRepository.get(systemId)
            ?: throw IllegalArgumentException("No existe el sistema $systemId")
        val controls = catalog.controls.associateBy { it.id }
        val owner = controls[requirement.controlId]
            ?: throw IllegalStateException("No existe el control de ${requirement.id}")
        return applicabilityCalculator.evaluate(owner, system, controls).status ==
            ApplicabilityStatus.APPLIES
    }

    private fun RequirementResponseEntity.toDomain(): RequirementResponse {
        val maturity = enumValue<MaturityLevel>(
            cipher.decrypt(
                maturityCiphertext,
                purpose(systemId, requirementId, "maturity"),
            ),
        )
        val note = noteCiphertext?.let {
            cipher.decrypt(it, purpose(systemId, requirementId, "note"))
        }
        return RequirementResponse(
            systemId = systemId,
            requirementId = requirementId,
            maturity = maturity,
            note = note,
            updatedAt = Instant.ofEpochMilli(updatedAt),
        )
    }

    private fun purpose(systemId: String, requirementId: String, field: String) =
        "response:$systemId:$requirementId:$field"

    private inline fun <reified T : Enum<T>> enumValue(value: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalStateException("Valor cifrado de madurez no válido")
}
