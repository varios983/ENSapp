package com.ensapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_revisions")
data class CatalogRevisionEntity(
    @PrimaryKey val revisionId: String,
    val catalogUuid: String,
    val version: String?,
    val oscalVersion: String?,
    val lastModified: String?,
    val checksum: String,
    val importedAt: Long,
    val active: Boolean,
)

@Entity(
    tableName = "frameworks",
    foreignKeys = [ForeignKey(
        entity = CatalogRevisionEntity::class,
        parentColumns = ["revisionId"],
        childColumns = ["revisionId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("revisionId")],
)
data class FrameworkEntity(
    @PrimaryKey val id: String,
    val revisionId: String,
    val title: String,
    val label: String?,
    val sortOrder: Int,
)

@Entity(
    tableName = "families",
    foreignKeys = [ForeignKey(
        entity = FrameworkEntity::class,
        parentColumns = ["id"],
        childColumns = ["frameworkId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("frameworkId")],
)
data class FamilyEntity(
    @PrimaryKey val id: String,
    val frameworkId: String,
    val title: String,
    val label: String?,
    val sortOrder: Int,
)

@Entity(
    tableName = "catalog_controls",
    foreignKeys = [
        ForeignKey(
            entity = CatalogRevisionEntity::class,
            parentColumns = ["revisionId"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = FrameworkEntity::class,
            parentColumns = ["id"],
            childColumns = ["frameworkId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CatalogControlEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentControlId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("revisionId"), Index("frameworkId"), Index("familyId"), Index("parentControlId")],
)
data class CatalogControlEntity(
    @PrimaryKey val id: String,
    val revisionId: String,
    val kind: String,
    val parentControlId: String?,
    val frameworkId: String,
    val familyId: String?,
    val title: String,
    val controlClass: String?,
    val label: String?,
    val sortOrder: Int,
)

@Entity(
    tableName = "catalog_properties",
    primaryKeys = ["ownerKind", "ownerId", "sourceOrder"],
    indices = [Index("ownerKind", "ownerId")],
)
data class CatalogPropertyEntity(
    val ownerKind: String,
    val ownerId: String,
    val namespace: String?,
    val name: String,
    val value: String,
    val sourceOrder: Int,
)

@Entity(
    tableName = "applicability_rules",
    foreignKeys = [ForeignKey(
        entity = CatalogControlEntity::class,
        parentColumns = ["id"],
        childColumns = ["controlId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("controlId")],
)
data class ApplicabilityRuleEntity(
    @PrimaryKey val controlId: String,
    val criterion: String,
)

@Entity(
    tableName = "applicability_values",
    primaryKeys = ["controlId", "valueKind", "sourceOrder"],
    foreignKeys = [ForeignKey(
        entity = CatalogControlEntity::class,
        parentColumns = ["id"],
        childColumns = ["controlId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("controlId")],
)
data class ApplicabilityValueEntity(
    val controlId: String,
    val valueKind: String,
    val value: String,
    val sourceOrder: Int,
)

@Entity(
    tableName = "catalog_parameters",
    foreignKeys = [ForeignKey(
        entity = CatalogControlEntity::class,
        parentColumns = ["id"],
        childColumns = ["controlId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("controlId")],
)
data class CatalogParameterEntity(
    @PrimaryKey val id: String,
    val controlId: String,
    val label: String?,
    val usage: String?,
    val type: String?,
    val scopeKind: String?,
    val scopeValue: String?,
    val selectionCardinality: String?,
    val sortOrder: Int,
)

@Entity(
    tableName = "disjunctive_choices",
    primaryKeys = ["parameterId", "reinforcementId"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogParameterEntity::class,
            parentColumns = ["id"],
            childColumns = ["parameterId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CatalogControlEntity::class,
            parentColumns = ["id"],
            childColumns = ["reinforcementId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("reinforcementId")],
)
data class DisjunctiveChoiceEntity(
    val parameterId: String,
    val reinforcementId: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "catalog_parts",
    foreignKeys = [
        ForeignKey(
            entity = CatalogRevisionEntity::class,
            parentColumns = ["revisionId"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CatalogControlEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerControlId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CatalogPartEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentPartId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("revisionId"), Index("ownerControlId"), Index("parentPartId")],
)
data class CatalogPartEntity(
    @PrimaryKey val id: String,
    val revisionId: String,
    val ownerControlId: String?,
    val ownerGroupId: String?,
    val parentPartId: String?,
    val name: String,
    val namespace: String?,
    val label: String?,
    val prose: String?,
    val sortOrder: Int,
    val sourceOrder: Int,
)

@Entity(
    tableName = "requirements",
    foreignKeys = [
        ForeignKey(
            entity = CatalogPartEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CatalogControlEntity::class,
            parentColumns = ["id"],
            childColumns = ["controlId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("controlId")],
)
data class RequirementEntity(
    @PrimaryKey val id: String,
    val controlId: String,
    val label: String?,
    val text: String,
    val sortOrder: Int,
)
