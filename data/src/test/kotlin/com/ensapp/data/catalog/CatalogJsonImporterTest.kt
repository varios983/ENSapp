package com.ensapp.data.catalog

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatalogJsonImporterTest {
    @Test
    fun `imports the complete ENS catalog without flattening source metadata`() {
        val source = Files.newBufferedReader(Path.of("..", "datos", "ENS_Anexo_II_rev_10.json"))
            .use { it.readText() }

        val catalog = CatalogJsonImporter().import(source)

        assertEquals(3, catalog.frameworks.size)
        assertEquals(15, catalog.families.size)
        assertEquals(73, catalog.controls.count { it.kind.name == "MEASURE" })
        assertEquals(133, catalog.controls.count { it.kind.name == "REINFORCEMENT" })
        assertEquals(467, catalog.parts.count { it.name == "item" })
        assertEquals(386, catalog.requirements.size)
        assertEquals(10, catalog.parameters.count { it.choices.isNotEmpty() })
        assertTrue(catalog.parts.any { !it.prose.isNullOrBlank() })
        assertEquals(10, catalog.parameters.count { it.selectionCardinality == "one" })
        assertEquals(
            setOf("op.acc.5", "op.acc.6", "mp.com.4", "mp.s.2"),
            catalog.parameters.map { it.controlId }.toSet(),
        )
        val reinforcementIds = catalog.controls
            .filter { it.kind.name == "REINFORCEMENT" }
            .map { it.id }
            .toSet()
        assertTrue(catalog.parameters.flatMap { it.choices }.all { it in reinforcementIds })
        val partsById = catalog.parts.associateBy { it.id }
        val items = catalog.parts.filter { it.name == "item" }
        assertEquals(386, items.count { partsById[it.parentPartId]?.name == "requisitos" })
        assertEquals(76, items.count { partsById[it.parentPartId]?.name == "item" })
        assertEquals(5, items.count { partsById[it.parentPartId]?.name == "overview" })
        assertEquals(
            items.filter { partsById[it.parentPartId]?.name == "requisitos" }.map { it.id }.toSet(),
            catalog.requirements.map { it.id }.toSet(),
        )
        val properties = catalog.frameworks.flatMap { it.properties } +
            catalog.families.flatMap { it.properties } +
            catalog.controls.flatMap { it.properties } +
            catalog.parameters.flatMap { it.properties } +
            catalog.parts.flatMap { it.properties }
        assertTrue(
            properties
                .groupBy { Triple(it.ownerId, it.namespace, it.name) }
                .values.any { it.size > 1 },
            "Las propiedades repetidas del catálogo deben conservarse",
        )
    }
}
