package com.ensapp.data.catalog

import com.ensapp.data.local.DisjunctiveSelectionDao
import com.ensapp.data.local.SystemDisjunctiveSelectionEntity
import com.ensapp.data.security.FieldCipher
import com.ensapp.domain.catalog.CatalogRepository
import com.ensapp.domain.catalog.DisjunctiveSelection
import com.ensapp.domain.catalog.DisjunctiveSelectionRepository
import java.time.Instant

class DisjunctiveSelectionRepositoryImpl(
    private val dao: DisjunctiveSelectionDao,
    private val catalogRepository: CatalogRepository,
    private val cipher: FieldCipher,
) : DisjunctiveSelectionRepository {
    override suspend fun selected(
        systemId: String,
        parameterId: String,
    ): DisjunctiveSelection? =
        dao.selection(systemId, parameterId)?.let { entity ->
            DisjunctiveSelection(
                systemId = entity.systemId,
                parameterId = entity.parameterId,
                reinforcementId = cipher.decrypt(
                    entity.selectedReinforcementCiphertext,
                    purpose(entity.systemId, entity.parameterId),
                ),
            )
        }

    override suspend fun save(selection: DisjunctiveSelection) {
        val parameter = catalogRepository.activeCatalog().parameters
            .firstOrNull { it.id == selection.parameterId }
            ?: throw IllegalArgumentException("No existe el parámetro ${selection.parameterId}")
        require(selection.reinforcementId in parameter.choices) {
            "El refuerzo no está declarado en las opciones del parámetro"
        }
        dao.upsert(
            SystemDisjunctiveSelectionEntity(
                systemId = selection.systemId,
                parameterId = selection.parameterId,
                selectedReinforcementCiphertext = cipher.encrypt(
                    selection.reinforcementId,
                    purpose(selection.systemId, selection.parameterId),
                ),
                updatedAt = Instant.now().toEpochMilli(),
            ),
        )
    }

    private fun purpose(systemId: String, parameterId: String) =
        "selection:$systemId:$parameterId"
}
