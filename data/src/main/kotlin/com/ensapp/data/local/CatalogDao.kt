package com.ensapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ensapp.domain.catalog.ApplicabilityCriterion
import com.ensapp.domain.catalog.Catalog
import java.time.Instant

@Dao
abstract class CatalogDao {
    @Upsert
    abstract suspend fun upsertRevisions(values: List<CatalogRevisionEntity>)

    @Upsert
    abstract suspend fun upsertFrameworks(values: List<FrameworkEntity>)

    @Upsert
    abstract suspend fun upsertFamilies(values: List<FamilyEntity>)

    @Upsert
    abstract suspend fun upsertControls(values: List<CatalogControlEntity>)

    @Upsert
    abstract suspend fun upsertProperties(values: List<CatalogPropertyEntity>)

    @Upsert
    abstract suspend fun upsertRules(values: List<ApplicabilityRuleEntity>)

    @Upsert
    abstract suspend fun upsertApplicabilityValues(values: List<ApplicabilityValueEntity>)

    @Upsert
    abstract suspend fun upsertParameters(values: List<CatalogParameterEntity>)

    @Upsert
    abstract suspend fun upsertChoices(values: List<DisjunctiveChoiceEntity>)

    @Upsert
    abstract suspend fun upsertParts(values: List<CatalogPartEntity>)

    @Upsert
    abstract suspend fun upsertRequirements(values: List<RequirementEntity>)

    @Query("DELETE FROM catalog_properties")
    abstract suspend fun clearProperties()

    @Query("DELETE FROM disjunctive_choices")
    abstract suspend fun clearChoices()

    @Query("UPDATE catalog_revisions SET active = 0 WHERE active = 1")
    abstract suspend fun deactivateRevisions()

    @Query("DELETE FROM applicability_values WHERE controlId = :controlId")
    abstract suspend fun deleteApplicabilityValues(controlId: String)

    @Query("DELETE FROM applicability_rules WHERE controlId = :controlId")
    abstract suspend fun deleteApplicabilityRule(controlId: String)

    @Query("SELECT * FROM catalog_revisions WHERE active = 1 LIMIT 1")
    abstract suspend fun activeRevision(): CatalogRevisionEntity?

    @Query("SELECT * FROM frameworks WHERE revisionId = :revisionId ORDER BY sortOrder")
    abstract suspend fun frameworks(revisionId: String): List<FrameworkEntity>

    @Query(
        "SELECT * FROM catalog_controls WHERE revisionId = :revisionId " +
            "ORDER BY frameworkId, familyId, parentControlId, sortOrder",
    )
    abstract suspend fun controls(revisionId: String): List<CatalogControlEntity>

    @Query(
        "SELECT f.* FROM families f INNER JOIN frameworks w ON w.id = f.frameworkId " +
            "WHERE w.revisionId = :revisionId ORDER BY w.sortOrder, f.sortOrder",
    )
    abstract suspend fun families(revisionId: String): List<FamilyEntity>

    @Query(
        "SELECT p.* FROM catalog_parameters p INNER JOIN catalog_controls c ON c.id = p.controlId " +
            "WHERE c.revisionId = :revisionId ORDER BY c.sortOrder, p.sortOrder",
    )
    abstract suspend fun parameters(revisionId: String): List<CatalogParameterEntity>

    @Query(
        "SELECT c.* FROM disjunctive_choices c INNER JOIN catalog_parameters p " +
            "ON p.id = c.parameterId INNER JOIN catalog_controls control " +
            "ON control.id = p.controlId WHERE control.revisionId = :revisionId " +
            "ORDER BY c.parameterId, c.sortOrder",
    )
    abstract suspend fun choices(revisionId: String): List<DisjunctiveChoiceEntity>

    @Query("SELECT * FROM catalog_parts WHERE revisionId = :revisionId ORDER BY sourceOrder")
    abstract suspend fun parts(revisionId: String): List<CatalogPartEntity>

    @Query(
        "SELECT r.* FROM requirements r INNER JOIN catalog_parts p ON p.id = r.id " +
            "WHERE p.revisionId = :revisionId ORDER BY p.sourceOrder",
    )
    abstract suspend fun requirements(revisionId: String): List<RequirementEntity>

    @Query("SELECT * FROM catalog_properties ORDER BY ownerKind, ownerId, sourceOrder")
    abstract suspend fun properties(): List<CatalogPropertyEntity>

    @Query(
        "SELECT r.* FROM applicability_rules r INNER JOIN catalog_controls c " +
            "ON c.id = r.controlId WHERE c.revisionId = :revisionId",
    )
    abstract suspend fun applicabilityRules(revisionId: String): List<ApplicabilityRuleEntity>

    @Query(
        "SELECT v.* FROM applicability_values v INNER JOIN catalog_controls c " +
            "ON c.id = v.controlId WHERE c.revisionId = :revisionId " +
            "ORDER BY v.controlId, v.valueKind, v.sourceOrder",
    )
    abstract suspend fun applicabilityValues(revisionId: String): List<ApplicabilityValueEntity>

    @Transaction
    open suspend fun import(catalog: Catalog, importedAt: Instant = Instant.now()) {
        deactivateRevisions()
        upsertRevisions(
            listOf(
                CatalogRevisionEntity(
                    revisionId = catalog.revision.revisionId,
                    catalogUuid = catalog.revision.catalogUuid,
                    version = catalog.revision.version,
                    oscalVersion = catalog.revision.oscalVersion,
                    lastModified = catalog.revision.lastModified,
                    checksum = catalog.revision.checksum,
                    importedAt = importedAt.toEpochMilli(),
                    active = true,
                ),
            ),
        )
        upsertFrameworks(
            catalog.frameworks.map {
                FrameworkEntity(
                    it.id,
                    catalog.revision.revisionId,
                    it.title,
                    it.label,
                    it.sortOrder,
                )
            },
        )
        upsertFamilies(
            catalog.families.map { FamilyEntity(it.id, it.frameworkId, it.title, it.label, it.sortOrder) },
        )
        upsertControls(
            catalog.controls.map {
                CatalogControlEntity(
                    id = it.id,
                    revisionId = catalog.revision.revisionId,
                    kind = it.kind.name,
                    parentControlId = it.parentControlId,
                    frameworkId = it.frameworkId,
                    familyId = it.familyId,
                    title = it.title,
                    controlClass = it.controlClass,
                    label = it.label,
                    sortOrder = it.sortOrder,
                )
            },
        )
        clearProperties()
        clearChoices()
        upsertParameters(
            catalog.parameters.map {
                CatalogParameterEntity(
                    id = it.id,
                    controlId = it.controlId,
                    label = it.label,
                    usage = it.usage,
                    type = it.type,
                    scopeKind = it.scopeKind,
                    scopeValue = it.scopeValue,
                    selectionCardinality = it.selectionCardinality,
                    sortOrder = it.sortOrder,
                )
            },
        )
        upsertParts(
            catalog.parts.mapIndexed { sourceOrder, it ->
                CatalogPartEntity(
                    id = it.id,
                    revisionId = catalog.revision.revisionId,
                    ownerControlId = it.ownerControlId,
                    ownerGroupId = it.ownerGroupId,
                    parentPartId = it.parentPartId,
                    name = it.name,
                    namespace = it.namespace,
                    label = it.label,
                    prose = it.prose,
                    sortOrder = it.sortOrder,
                    sourceOrder = sourceOrder,
                )
            },
        )
        upsertRequirements(
            catalog.requirements.map {
                RequirementEntity(it.id, it.controlId, it.label, it.text, it.sortOrder)
            },
        )
        upsertProperties(catalog.allProperties().map {
            CatalogPropertyEntity(
                ownerKind = it.ownerKind,
                ownerId = it.ownerId,
                namespace = it.namespace,
                name = it.name,
                value = it.value,
                sourceOrder = it.sourceOrder,
            )
        })
        catalog.controls.forEach { control ->
            deleteApplicabilityRule(control.id)
            deleteApplicabilityValues(control.id)
        }
        upsertRules(
            catalog.controls.mapNotNull { control ->
                control.applicability?.let {
                    ApplicabilityRuleEntity(control.id, it.criterion.name)
                }
            },
        )
        upsertApplicabilityValues(
            catalog.controls.flatMap { control ->
                val rule = control.applicability ?: return@flatMap emptyList()
                val values = when (rule.criterion) {
                    ApplicabilityCriterion.CATEGORY ->
                        rule.categories.map { "CATEGORY" to it.name }
                    ApplicabilityCriterion.DIMENSION_LEVEL ->
                        rule.dimensions.map { "DIMENSION" to it.name } +
                            rule.levels.map { "LEVEL" to it.name }
                    else -> emptyList()
                }
                values.mapIndexed { index, (kind, value) ->
                    ApplicabilityValueEntity(control.id, kind, value, index)
                }
            },
        )
        upsertChoices(
            catalog.parameters.flatMap { parameter ->
                parameter.choices.mapIndexed { index, reinforcementId ->
                    DisjunctiveChoiceEntity(parameter.id, reinforcementId, index)
                }
            },
        )
    }

    private fun Catalog.allProperties() =
        frameworks.flatMap { it.properties } +
            families.flatMap { it.properties } +
            controls.flatMap { it.properties } +
            parameters.flatMap { it.properties } +
            parts.flatMap { it.properties }
}
