package com.ensapp.data.catalog

import com.ensapp.data.local.ApplicabilityValueEntity
import com.ensapp.data.local.CatalogDao
import com.ensapp.data.local.CatalogPropertyEntity
import com.ensapp.domain.catalog.ApplicabilityCriterion
import com.ensapp.domain.catalog.ApplicabilityRule
import com.ensapp.domain.catalog.Catalog
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogParameter
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogProperty
import com.ensapp.domain.catalog.CatalogRepository
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.Family
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.Requirement
import com.ensapp.domain.catalog.SecurityDimension

class CatalogRepositoryImpl(
    private val dao: CatalogDao,
) : CatalogRepository {
    override suspend fun activeCatalog(): Catalog {
        val revision = dao.activeRevision()
            ?: throw IllegalStateException("El catálogo ENS aún no se ha importado")
        val revisionId = revision.revisionId
        val properties = dao.properties()
            .groupBy { it.ownerKind to it.ownerId }
            .mapValues { (_, values) -> values.map { it.toDomain() } }
        val rules = dao.applicabilityRules(revisionId).associateBy { it.controlId }
        val values = dao.applicabilityValues(revisionId).groupBy { it.controlId }
        val parameters = dao.parameters(revisionId)
        val choices = dao.choices(revisionId)
            .groupBy { it.parameterId }
            .mapValues { (_, values) -> values.sortedBy { it.sortOrder }.map { it.reinforcementId } }

        val frameworks = dao.frameworks(revisionId).map {
            Framework(
                id = it.id,
                title = it.title,
                label = it.label,
                sortOrder = it.sortOrder,
                properties = properties["GROUP" to it.id].orEmpty(),
            )
        }
        val families = dao.families(revisionId).map {
            Family(
                id = it.id,
                frameworkId = it.frameworkId,
                title = it.title,
                label = it.label,
                sortOrder = it.sortOrder,
                properties = properties["GROUP" to it.id].orEmpty(),
            )
        }
        val controls = dao.controls(revisionId).map { entity ->
            CatalogControl(
                id = entity.id,
                kind = enumValue(entity.kind, "tipo de control"),
                parentControlId = entity.parentControlId,
                frameworkId = entity.frameworkId,
                familyId = entity.familyId,
                title = entity.title,
                controlClass = entity.controlClass,
                label = entity.label,
                sortOrder = entity.sortOrder,
                properties = properties["CONTROL" to entity.id].orEmpty(),
                applicability = rules[entity.id]?.let { rule ->
                    rule.toDomain(values[entity.id].orEmpty())
                },
            )
        }
        val catalogParameters = parameters.map { entity ->
            CatalogParameter(
                id = entity.id,
                controlId = entity.controlId,
                label = entity.label,
                usage = entity.usage,
                type = entity.type,
                scopeKind = entity.scopeKind,
                scopeValue = entity.scopeValue,
                selectionCardinality = entity.selectionCardinality,
                sortOrder = entity.sortOrder,
                properties = properties["PARAMETER" to entity.id].orEmpty(),
                choices = choices[entity.id].orEmpty(),
            )
        }
        val parts = dao.parts(revisionId).map { entity ->
            CatalogPart(
                id = entity.id,
                ownerControlId = entity.ownerControlId,
                ownerGroupId = entity.ownerGroupId,
                parentPartId = entity.parentPartId,
                name = entity.name,
                namespace = entity.namespace,
                label = entity.label,
                prose = entity.prose,
                sortOrder = entity.sortOrder,
                properties = properties["PART" to entity.id].orEmpty(),
            )
        }
        val requirements = dao.requirements(revisionId).map {
            Requirement(it.id, it.controlId, it.label, it.text, it.sortOrder)
        }

        return Catalog(
            revision = CatalogRevision(
                revisionId = revision.revisionId,
                catalogUuid = revision.catalogUuid,
                version = revision.version,
                oscalVersion = revision.oscalVersion,
                lastModified = revision.lastModified,
                checksum = revision.checksum,
            ),
            frameworks = frameworks,
            families = families,
            controls = controls,
            parameters = catalogParameters,
            parts = parts,
            requirements = requirements,
        )
    }

    private fun com.ensapp.data.local.ApplicabilityRuleEntity.toDomain(
        values: List<ApplicabilityValueEntity>,
    ): ApplicabilityRule {
        val criterion = enumValue<ApplicabilityCriterion>(criterion, "criterio de aplicabilidad")
        return ApplicabilityRule(
            criterion = criterion,
            categories = values.filter { it.valueKind == "CATEGORY" }
                .map { enumValue<EnsCategory>(it.value, "categoría") }.toSet(),
            dimensions = values.filter { it.valueKind == "DIMENSION" }
                .map { enumValue<SecurityDimension>(it.value, "dimensión") }.toSet(),
            levels = values.filter { it.valueKind == "LEVEL" }
                .map { enumValue<DimensionLevel>(it.value, "nivel") }.toSet(),
        )
    }

    private fun CatalogPropertyEntity.toDomain() = CatalogProperty(
        ownerKind = ownerKind,
        ownerId = ownerId,
        namespace = namespace,
        name = name,
        value = value,
        sourceOrder = sourceOrder,
    )

    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalStateException("Valor almacenado no válido para $field")
}
