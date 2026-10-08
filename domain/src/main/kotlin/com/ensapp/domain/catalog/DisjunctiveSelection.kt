package com.ensapp.domain.catalog

import com.ensapp.domain.system.InformationSystem

data class DisjunctiveSelection(
    val systemId: String,
    val parameterId: String,
    val reinforcementId: String,
)

interface DisjunctiveSelectionRepository {
    suspend fun selected(systemId: String, parameterId: String): DisjunctiveSelection?
    suspend fun save(selection: DisjunctiveSelection)
}

object DisjunctiveSelectionApplicability {
    fun evaluate(
        reinforcementId: String,
        baseEvaluation: ApplicabilityEvaluation,
        parameters: List<CatalogParameter>,
        system: InformationSystem,
        selectedByParameter: Map<String, String>,
    ): ApplicabilityEvaluation {
        if (baseEvaluation.status == ApplicabilityStatus.DOES_NOT_APPLY) return baseEvaluation
        val declaringParameters = parameters.filter {
            reinforcementId in it.choices && it.type == "seleccion-refuerzo"
        }
        if (declaringParameters.isEmpty()) return baseEvaluation

        val inScope = declaringParameters.filter { parameter ->
            when (parameter.scopeKind) {
                "categoria" ->
                    parameter.scopeValue?.equals(system.category.name, ignoreCase = true) == true
                "nivel" -> system.dimensions.values.any {
                    parameter.scopeValue?.equals(it.name, ignoreCase = true) == true
                }
                else -> true
            }
        }
        if (inScope.isEmpty()) {
            return baseEvaluation.copy(
                status = ApplicabilityStatus.DOES_NOT_APPLY,
                reason = "El ámbito del parámetro no corresponde a este sistema",
                provenance = ApplicabilityProvenance.OWN,
            )
        }
        if (inScope.any { selectedByParameter[it.id] == reinforcementId }) return baseEvaluation
        if (inScope.any { selectedByParameter[it.id] == null }) {
            return baseEvaluation.copy(
                status = ApplicabilityStatus.REVIEW,
                reason = "Debe seleccionarse una alternativa disyuntiva",
                provenance = ApplicabilityProvenance.UNSPECIFIED,
            )
        }
        return baseEvaluation.copy(
            status = ApplicabilityStatus.DOES_NOT_APPLY,
            reason = "Se seleccionó otra alternativa disyuntiva",
            provenance = ApplicabilityProvenance.OWN,
        )
    }
}
