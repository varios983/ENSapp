package com.ensapp.domain.catalog

class RequirementClassifier {
    fun classify(parts: List<CatalogPart>): List<Requirement> {
        val partsById = parts.associateBy { it.id }
        require(partsById.size == parts.size) { "Los IDs de CatalogPart deben ser únicos" }

        return parts.mapNotNull { part ->
            if (part.name != "item" || part.parentPartId == null) return@mapNotNull null
            val parent = partsById[part.parentPartId]
                ?: throw IllegalArgumentException("Falta la parte padre de ${part.id}")
            if (parent.name != "requisitos") return@mapNotNull null
            val controlId = part.ownerControlId
                ?: throw IllegalArgumentException("El requisito ${part.id} no tiene control propietario")
            Requirement(
                id = part.id,
                controlId = controlId,
                label = part.label,
                text = part.prose.orEmpty(),
                sortOrder = part.sortOrder,
            )
        }
    }
}
