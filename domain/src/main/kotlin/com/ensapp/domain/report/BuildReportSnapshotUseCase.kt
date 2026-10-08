package com.ensapp.domain.report

import com.ensapp.domain.catalog.BrowseCatalogUseCases
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.ControlBrowseNode
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.Family
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.status.GetComplianceSummaryUseCase
import com.ensapp.domain.status.RequirementComplianceRecord
import com.ensapp.domain.system.InformationSystemRepository

class BuildReportSnapshotUseCase(
    private val systemRepository: InformationSystemRepository,
    private val browseCatalog: BrowseCatalogUseCases,
    private val getComplianceSummary: GetComplianceSummaryUseCase,
) {
    suspend fun execute(systemId: String): ReportSnapshot {
        require(systemId.isNotBlank()) { "Falta el sistema de información" }
        val system = systemRepository.get(systemId)
            ?: throw IllegalArgumentException("No existe el sistema $systemId")
        val tree = browseCatalog.execute(system)
        val rows = buildList {
            fun addControl(
                node: ControlBrowseNode,
                framework: Framework,
                family: Family?,
                measure: CatalogControl?,
            ) {
                val ownMeasure = if (node.control.kind == ControlKind.MEASURE) {
                    node.control
                } else {
                    measure
                }
                val reinforcement = node.control.takeIf {
                    it.kind == ControlKind.REINFORCEMENT
                }
                if (ownMeasure != null) {
                    node.requirements.forEach { item ->
                        add(
                            ReportRequirement(
                                framework = framework,
                                family = family,
                                measure = ownMeasure,
                                reinforcement = reinforcement,
                                item = item.copy(
                                    response = item.response?.takeIf {
                                        it.systemId == system.id
                                    },
                                ),
                            ),
                        )
                    }
                }
                node.children.forEach { child ->
                    addControl(child, framework, family, ownMeasure)
                }
            }

            tree.frameworks.forEach { frameworkNode ->
                frameworkNode.controls.forEach { node ->
                    addControl(node, frameworkNode.framework, null, null)
                }
                frameworkNode.families.forEach { familyNode ->
                    familyNode.controls.forEach { node ->
                        addControl(
                            node,
                            frameworkNode.framework,
                            familyNode.family,
                            null,
                        )
                    }
                }
            }
        }
        val summary = getComplianceSummary.calculate(
            rows.map { row ->
                RequirementComplianceRecord(
                    requirementId = row.item.requirement.id,
                    applicability = row.item.applicability,
                    dimensions = row.item.dimensions,
                    response = row.item.response,
                )
            },
        )
        return ReportSnapshot(system, tree.revision, summary, rows)
    }
}
