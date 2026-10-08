package com.ensapp.domain.catalog

enum class EnsCategory {
    BASICA,
    MEDIA,
    ALTA,
}

enum class SecurityDimension {
    CONFIDENCIALIDAD,
    INTEGRIDAD,
    DISPONIBILIDAD,
    AUTENTICIDAD,
    TRAZABILIDAD,
}

enum class DimensionLevel {
    BAJO,
    MEDIO,
    ALTO,
}

enum class ControlKind {
    MEASURE,
    REINFORCEMENT,
}

enum class ApplicabilityCriterion {
    CATEGORY,
    DIMENSION_LEVEL,
    INHERITED_FROM_MEASURE,
    UNSPECIFIED,
}

enum class ApplicabilityProvenance {
    OWN,
    INHERITED_FROM_MEASURE,
    UNSPECIFIED,
}

data class Framework(
    val id: String,
    val title: String,
    val label: String?,
    val sortOrder: Int,
    val properties: List<CatalogProperty>,
)

data class Family(
    val id: String,
    val frameworkId: String,
    val title: String,
    val label: String?,
    val sortOrder: Int,
    val properties: List<CatalogProperty>,
)

data class CatalogProperty(
    val ownerKind: String,
    val ownerId: String,
    val namespace: String?,
    val name: String,
    val value: String,
    val sourceOrder: Int,
)

data class ApplicabilityRule(
    val criterion: ApplicabilityCriterion,
    val categories: Set<EnsCategory> = emptySet(),
    val dimensions: Set<SecurityDimension> = emptySet(),
    val levels: Set<DimensionLevel> = emptySet(),
    val provenance: ApplicabilityProvenance = ApplicabilityProvenance.OWN,
)

data class CatalogControl(
    val id: String,
    val kind: ControlKind,
    val parentControlId: String?,
    val frameworkId: String,
    val familyId: String?,
    val title: String,
    val controlClass: String?,
    val label: String?,
    val sortOrder: Int,
    val properties: List<CatalogProperty>,
    val applicability: ApplicabilityRule?,
)

data class CatalogParameter(
    val id: String,
    val controlId: String,
    val label: String?,
    val usage: String?,
    val type: String?,
    val scopeKind: String?,
    val scopeValue: String?,
    val selectionCardinality: String?,
    val sortOrder: Int,
    val properties: List<CatalogProperty>,
    val choices: List<String>,
)

data class CatalogPart(
    val id: String,
    val ownerControlId: String?,
    val ownerGroupId: String?,
    val parentPartId: String?,
    val name: String,
    val namespace: String?,
    val label: String?,
    val prose: String?,
    val sortOrder: Int,
    val properties: List<CatalogProperty>,
)

data class Requirement(
    val id: String,
    val controlId: String,
    val label: String?,
    val text: String,
    val sortOrder: Int,
)

data class CatalogRevision(
    val revisionId: String,
    val catalogUuid: String,
    val version: String?,
    val oscalVersion: String?,
    val lastModified: String?,
    val checksum: String,
)

data class Catalog(
    val revision: CatalogRevision,
    val frameworks: List<Framework>,
    val families: List<Family>,
    val controls: List<CatalogControl>,
    val parameters: List<CatalogParameter>,
    val parts: List<CatalogPart>,
    val requirements: List<Requirement>,
)

data class ApplicabilityEvaluation(
    val status: ApplicabilityStatus,
    val reason: String?,
    val provenance: ApplicabilityProvenance,
)

enum class ApplicabilityStatus {
    APPLIES,
    DOES_NOT_APPLY,
    REVIEW,
}
