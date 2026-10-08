package com.ensapp.domain.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequirementClassifierTest {
    @Test
    fun `only item children of requisitos are response requirements`() {
        val parts = listOf(
            part("requirements", "requisitos"),
            part("main", "item", parent = "requirements", control = "op.acc.1"),
            part("subitem", "item", parent = "main", control = "op.acc.1"),
            part("overview", "overview"),
            part("description", "item", parent = "overview", control = "op.acc.1"),
        )

        val requirements = RequirementClassifier().classify(parts)

        assertEquals(listOf("main"), requirements.map { it.id })
    }

    private fun part(
        id: String,
        name: String,
        parent: String? = null,
        control: String? = null,
    ) = CatalogPart(
        id = id,
        ownerControlId = control,
        ownerGroupId = null,
        parentPartId = parent,
        name = name,
        namespace = null,
        label = null,
        prose = id,
        sortOrder = 0,
        properties = emptyList(),
    )
}
