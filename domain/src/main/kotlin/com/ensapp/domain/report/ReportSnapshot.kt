package com.ensapp.domain.report

import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.Family
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.status.ComplianceSummary
import com.ensapp.domain.system.InformationSystem

data class ReportRequirement(
    val framework: Framework,
    val family: Family?,
    val measure: CatalogControl,
    val reinforcement: CatalogControl?,
    val item: RequirementBrowseItem,
)

data class ReportSnapshot(
    val system: InformationSystem,
    val revision: CatalogRevision,
    val summary: ComplianceSummary,
    val requirements: List<ReportRequirement>,
)
