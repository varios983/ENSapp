package com.ensapp.data.catalog

import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.RequirementClassifier
import com.ensapp.domain.catalog.Requirement

class RequirementImportMapper {
    fun map(parts: List<CatalogPart>): List<Requirement> =
        RequirementClassifier().classify(parts)
}
