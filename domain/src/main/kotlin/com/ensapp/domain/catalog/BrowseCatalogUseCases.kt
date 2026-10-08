package com.ensapp.domain.catalog

import com.ensapp.domain.system.InformationSystem
import com.ensapp.domain.response.RequirementResponse
import com.ensapp.domain.response.RequirementResponseRepository

data class CatalogPartNode(
    val part: CatalogPart,
    val children: List<CatalogPartNode>,
)

data class RequirementBrowseItem(
    val requirement: Requirement,
    val applicability: ApplicabilityEvaluation,
    val dimensions: Set<SecurityDimension>,
    val response: RequirementResponse?,
    val content: CatalogPartNode,
)

enum class RequirementFilterStatus {
    PENDING,
    ANSWERED,
}

data class RequirementListFilter(
    val status: RequirementFilterStatus,
    val dimension: SecurityDimension? = null,
)

data class ControlBrowseNode(
    val control: CatalogControl,
    val applicability: ApplicabilityEvaluation,
    val parameters: List<CatalogParameter>,
    val requirements: List<RequirementBrowseItem>,
    val children: List<ControlBrowseNode>,
)

data class FamilyBrowseNode(
    val family: Family,
    val applicability: ApplicabilityEvaluation,
    val controls: List<ControlBrowseNode>,
)

data class FrameworkBrowseNode(
    val framework: Framework,
    val applicability: ApplicabilityEvaluation,
    val families: List<FamilyBrowseNode>,
    val controls: List<ControlBrowseNode>,
)

data class CatalogBrowseTree(
    val revision: CatalogRevision,
    val frameworks: List<FrameworkBrowseNode>,
)

class BrowseCatalogUseCases(
    private val catalogRepository: CatalogRepository,
    private val applicabilityCalculator: ApplicabilityCalculator = ApplicabilityCalculator(),
    private val selectionRepository: DisjunctiveSelectionRepository? = null,
    private val responseRepository: RequirementResponseRepository? = null,
) {
    suspend fun execute(system: InformationSystem): CatalogBrowseTree {
        val catalog = catalogRepository.activeCatalog()
        val controlsById = catalog.controls.associateBy { it.id }
        val requirementsByControl = catalog.requirements.groupBy { it.controlId }
        val parametersByControl = catalog.parameters.groupBy { it.controlId }
        val selectedByParameter = if (selectionRepository == null) {
            emptyMap()
        } else {
            catalog.parameters.mapNotNull { parameter ->
                selectionRepository.selected(system.id, parameter.id)?.let {
                    parameter.id to it.reinforcementId
                }
            }.toMap()
        }
        val responsesByRequirement = responseRepository
            ?.list(system.id)
            ?.associateBy { it.requirementId }
            .orEmpty()
        val partsByParent = catalog.parts
            .filter { it.parentPartId != null }
            .groupBy { it.parentPartId }
            .mapValues { (_, children) -> children.sortedBy { it.sortOrder } }
        val partsById = catalog.parts.associateBy { it.id }

        fun partTree(partId: String): CatalogPartNode {
            val part = partsById[partId]
                ?: throw IllegalStateException("Falta la parte del requisito $partId")
            return CatalogPartNode(
                part = part,
                children = partsByParent[partId].orEmpty().map { partTree(it.id) },
            )
        }

        fun controlTree(control: CatalogControl): ControlBrowseNode {
            val baseEvaluation = applicabilityCalculator.evaluate(control, system, controlsById)
            val evaluation = DisjunctiveSelectionApplicability.evaluate(
                reinforcementId = control.id,
                baseEvaluation = baseEvaluation,
                parameters = catalog.parameters,
                system = system,
                selectedByParameter = selectedByParameter,
            )
            val requirementNodes = requirementsByControl[control.id].orEmpty().map { requirement ->
                RequirementBrowseItem(
                    requirement = requirement,
                    applicability = evaluation,
                    dimensions = applicabilityCalculator.effectiveDimensions(
                        control,
                        controlsById,
                    ),
                    response = responsesByRequirement[requirement.id],
                    content = partTree(requirement.id),
                )
            }
            val children = catalog.controls
                .filter { it.parentControlId == control.id }
                .sortedBy { it.sortOrder }
                .map(::controlTree)
            return ControlBrowseNode(
                control = control,
                applicability = evaluation,
                parameters = parametersByControl[control.id].orEmpty().sortedBy { it.sortOrder },
                requirements = requirementNodes,
                children = children,
            )
        }

        val familyNodes = catalog.families.associate { family ->
            val controls = catalog.controls
                .filter { it.familyId == family.id && it.parentControlId == null }
                .sortedBy { it.sortOrder }
                .map(::controlTree)
            family.id to FamilyBrowseNode(
                family = family,
                applicability = aggregate(controls.map { it.applicability }),
                controls = controls,
            )
        }
        val frameworks = catalog.frameworks.map { framework ->
            val families = catalog.families
                .filter { it.frameworkId == framework.id }
                .sortedBy { it.sortOrder }
                .map { familyNodes.getValue(it.id) }
            val controls = catalog.controls
                .filter {
                    it.frameworkId == framework.id &&
                        it.familyId == null &&
                        it.parentControlId == null
                }
                .sortedBy { it.sortOrder }
                .map(::controlTree)
            FrameworkBrowseNode(
                framework = framework,
                applicability = aggregate(
                    families.map { it.applicability } + controls.map { it.applicability },
                ),
                families = families,
                controls = controls,
            )
        }
        return CatalogBrowseTree(catalog.revision, frameworks)
    }

    private fun aggregate(evaluations: List<ApplicabilityEvaluation>): ApplicabilityEvaluation =
        when {
            evaluations.any { it.status == ApplicabilityStatus.APPLIES } ->
                ApplicabilityEvaluation(
                    ApplicabilityStatus.APPLIES,
                    "Hay elementos aplicables",
                    ApplicabilityProvenance.OWN,
                )
            evaluations.any { it.status == ApplicabilityStatus.REVIEW } ->
                ApplicabilityEvaluation(
                    ApplicabilityStatus.REVIEW,
                    "Hay reglas que requieren revisión",
                    ApplicabilityProvenance.UNSPECIFIED,
                )
            else -> ApplicabilityEvaluation(
                ApplicabilityStatus.DOES_NOT_APPLY,
                "Ningún elemento aplica",
                ApplicabilityProvenance.OWN,
            )
        }
}
