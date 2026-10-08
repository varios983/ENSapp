package com.ensapp.data.status

import com.ensapp.domain.catalog.ApplicabilityCalculator
import com.ensapp.domain.catalog.DisjunctiveSelectionApplicability
import com.ensapp.domain.catalog.DisjunctiveSelectionRepository
import com.ensapp.domain.catalog.CatalogRepository
import com.ensapp.domain.response.RequirementResponseRepository
import com.ensapp.domain.status.ComplianceStatusRepository
import com.ensapp.domain.status.RequirementComplianceRecord
import com.ensapp.domain.system.InformationSystemRepository

class ComplianceStatusRepositoryImpl(
    private val catalogRepository: CatalogRepository,
    private val systemRepository: InformationSystemRepository,
    private val responseRepository: RequirementResponseRepository,
    private val selectionRepository: DisjunctiveSelectionRepository,
    private val applicabilityCalculator: ApplicabilityCalculator = ApplicabilityCalculator(),
) : ComplianceStatusRepository {
    override suspend fun records(systemId: String): List<RequirementComplianceRecord> {
        val system = systemRepository.get(systemId)
            ?: throw IllegalArgumentException("No existe el sistema $systemId")
        val catalog = catalogRepository.activeCatalog()
        val controlsById = catalog.controls.associateBy { it.id }
        val selectedByParameter = catalog.parameters.mapNotNull { parameter ->
            selectionRepository.selected(systemId, parameter.id)?.let {
                parameter.id to it.reinforcementId
            }
        }.toMap()
        val responses = responseRepository.list(systemId).associateBy { it.requirementId }

        return catalog.requirements.map { requirement ->
            val control = controlsById[requirement.controlId]
                ?: throw IllegalStateException("No existe el control de ${requirement.id}")
            val evaluation = DisjunctiveSelectionApplicability.evaluate(
                reinforcementId = control.id,
                baseEvaluation = applicabilityCalculator.evaluate(control, system, controlsById),
                parameters = catalog.parameters,
                system = system,
                selectedByParameter = selectedByParameter,
            )
            RequirementComplianceRecord(
                requirementId = requirement.id,
                applicability = evaluation,
                dimensions = applicabilityCalculator.effectiveDimensions(control, controlsById),
                response = responses[requirement.id],
            )
        }
    }
}
