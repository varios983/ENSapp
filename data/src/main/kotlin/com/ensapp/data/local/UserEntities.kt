package com.ensapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "information_systems")
data class InformationSystemEntity(
    @androidx.room.PrimaryKey val id: String,
    val nameCiphertext: ByteArray,
    val categoryCiphertext: ByteArray,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "system_dimensions",
    primaryKeys = ["systemId", "dimension"],
    foreignKeys = [ForeignKey(
        entity = InformationSystemEntity::class,
        parentColumns = ["id"],
        childColumns = ["systemId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("systemId")],
)
data class SystemDimensionEntity(
    val systemId: String,
    val dimension: String,
    val levelCiphertext: ByteArray,
)

@Entity(
    tableName = "requirement_responses",
    primaryKeys = ["systemId", "requirementId"],
    foreignKeys = [
        ForeignKey(
            entity = InformationSystemEntity::class,
            parentColumns = ["id"],
            childColumns = ["systemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RequirementEntity::class,
            parentColumns = ["id"],
            childColumns = ["requirementId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("requirementId")],
)
data class RequirementResponseEntity(
    val systemId: String,
    val requirementId: String,
    val maturityCiphertext: ByteArray,
    val noteCiphertext: ByteArray?,
    val updatedAt: Long,
)

@Entity(
    tableName = "system_disjunctive_selections",
    primaryKeys = ["systemId", "parameterId"],
    foreignKeys = [
        ForeignKey(
            entity = InformationSystemEntity::class,
            parentColumns = ["id"],
            childColumns = ["systemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogParameterEntity::class,
            parentColumns = ["id"],
            childColumns = ["parameterId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("parameterId")],
)
data class SystemDisjunctiveSelectionEntity(
    val systemId: String,
    val parameterId: String,
    val selectedReinforcementCiphertext: ByteArray,
    val updatedAt: Long,
)
