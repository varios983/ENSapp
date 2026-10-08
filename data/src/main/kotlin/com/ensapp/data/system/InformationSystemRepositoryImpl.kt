package com.ensapp.data.system

import com.ensapp.data.local.InformationSystemDao
import com.ensapp.data.local.InformationSystemEntity
import com.ensapp.data.local.SystemDimensionEntity
import com.ensapp.data.security.FieldCipher
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.system.InformationSystem
import com.ensapp.domain.system.InformationSystemRepository
import java.time.Instant

class InformationSystemRepositoryImpl(
    private val dao: InformationSystemDao,
    private val cipher: FieldCipher,
) : InformationSystemRepository {
    override suspend fun save(system: InformationSystem) {
        val entity = InformationSystemEntity(
            id = system.id,
            nameCiphertext = cipher.encrypt(system.name, purpose(system.id, "name")),
            categoryCiphertext = cipher.encrypt(
                system.category.name,
                purpose(system.id, "category"),
            ),
            createdAt = system.createdAt.toEpochMilli(),
            updatedAt = system.updatedAt.toEpochMilli(),
        )
        val dimensions = system.dimensions.map { (dimension, level) ->
            SystemDimensionEntity(
                systemId = system.id,
                dimension = dimension.name,
                levelCiphertext = cipher.encrypt(
                    level.name,
                    purpose(system.id, "dimension:${dimension.name}"),
                ),
            )
        }
        dao.upsertSystemWithDimensions(entity, dimensions)
    }

    override suspend fun get(id: String): InformationSystem? {
        val entity = dao.system(id) ?: return null
        return entity.toDomain(dao.dimensions(id))
    }

    override suspend fun list(): List<InformationSystem> =
        dao.systems().map { it.toDomain(dao.dimensions(it.id)) }

    private fun InformationSystemEntity.toDomain(
        dimensions: List<SystemDimensionEntity>,
    ): InformationSystem {
        val decryptedDimensions = dimensions.associate { entity ->
            val dimension = enumValue<SecurityDimension>(entity.dimension, "dimensión")
            dimension to enumValue<DimensionLevel>(
                cipher.decrypt(
                    entity.levelCiphertext,
                    purpose(id, "dimension:${dimension.name}"),
                ),
                "nivel dimensional",
            )
        }
        return InformationSystem(
            id = id,
            name = cipher.decrypt(nameCiphertext, purpose(id, "name")),
            category = enumValue(
                cipher.decrypt(categoryCiphertext, purpose(id, "category")),
                "categoría",
            ),
            dimensions = decryptedDimensions,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
        )
    }

    private fun purpose(systemId: String, field: String) = "system:$systemId:$field"

    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalStateException("Valor cifrado no válido para $field")
}
