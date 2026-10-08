package com.ensapp.domain.catalog

import com.ensapp.domain.system.InformationSystem

class ApplicabilityCalculator {
    fun effectiveDimensions(
        control: CatalogControl,
        controlsById: Map<String, CatalogControl>,
    ): Set<SecurityDimension> = effectiveDimensions(control, controlsById, mutableSetOf())

    private fun effectiveDimensions(
        control: CatalogControl,
        controlsById: Map<String, CatalogControl>,
        visited: MutableSet<String>,
    ): Set<SecurityDimension> {
        if (!visited.add(control.id)) return emptySet()
        val rule = control.applicability
        if (rule?.criterion == ApplicabilityCriterion.DIMENSION_LEVEL) return rule.dimensions
        if (!ApplicabilityPolicy.shouldInherit(control, rule)) return emptySet()
        val parent = control.parentControlId?.let(controlsById::get) ?: return emptySet()
        return effectiveDimensions(parent, controlsById, visited)
    }

    fun evaluate(
        control: CatalogControl,
        system: InformationSystem,
        controlsById: Map<String, CatalogControl> = emptyMap(),
    ): ApplicabilityEvaluation = evaluate(control, system, controlsById, mutableSetOf())

    private fun evaluate(
        control: CatalogControl,
        system: InformationSystem,
        controlsById: Map<String, CatalogControl>,
        visited: MutableSet<String>,
    ): ApplicabilityEvaluation {
        if (!visited.add(control.id)) return review("Ciclo en la jerarquía de controles")
        val rule = control.applicability
        if (rule == null || rule.criterion == ApplicabilityCriterion.UNSPECIFIED) {
            if (!ApplicabilityPolicy.shouldInherit(control, rule)) {
                return review("El catálogo no especifica aplicabilidad para ${control.id}")
            }
            val parent = control.parentControlId?.let(controlsById::get)
                ?: return review("No se encuentra la medida padre de ${control.id}")
            return evaluate(parent, system, controlsById, visited).copy(
                reason = "Aplicabilidad heredada de ${parent.id}",
                provenance = ApplicabilityProvenance.INHERITED_FROM_MEASURE,
            )
        }

        return when (rule.criterion) {
            ApplicabilityCriterion.CATEGORY -> {
                if (rule.categories.isEmpty()) {
                    review("Faltan categorías de aplicabilidad para ${control.id}")
                } else if (system.category in rule.categories) {
                    applies("Categoría ${system.category} incluida")
                } else {
                    doesNotApply("Categoría ${system.category} no incluida")
                }
            }
            ApplicabilityCriterion.DIMENSION_LEVEL -> {
                if (rule.dimensions.isEmpty() || rule.levels.isEmpty()) {
                    review("Faltan dimensiones o niveles de aplicabilidad para ${control.id}")
                } else {
                    val matchingDimensions = rule.dimensions.filter { dimension ->
                        system.dimensions[dimension] in rule.levels
                    }
                    if (matchingDimensions.isNotEmpty()) {
                        applies(
                            "Nivel configurado coincidente en ${matchingDimensions.joinToString()}",
                        )
                    } else {
                        doesNotApply("Ningún nivel configurado coincide con los niveles del catálogo")
                    }
                }
            }
            ApplicabilityCriterion.INHERITED_FROM_MEASURE ->
                review("La procedencia heredada requiere la regla de la medida padre")
            ApplicabilityCriterion.UNSPECIFIED -> error("Criterio ya procesado")
        }
    }

    private fun applies(reason: String) = ApplicabilityEvaluation(
        status = ApplicabilityStatus.APPLIES,
        reason = reason,
        provenance = ApplicabilityProvenance.OWN,
    )

    private fun doesNotApply(reason: String) = ApplicabilityEvaluation(
        status = ApplicabilityStatus.DOES_NOT_APPLY,
        reason = reason,
        provenance = ApplicabilityProvenance.OWN,
    )

    private fun review(reason: String) = ApplicabilityEvaluation(
        status = ApplicabilityStatus.REVIEW,
        reason = reason,
        provenance = ApplicabilityProvenance.UNSPECIFIED,
    )
}
