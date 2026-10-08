package com.ensapp.data.catalog

import com.ensapp.domain.catalog.RequirementClassifier
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RequirementImportMappingTest {
    @Test
    fun `real catalog maps only official top-level requirement items`() {
        val source = Files.newBufferedReader(Path.of("..", "datos", "ENS_Anexo_II_rev_10.json"))
            .use { it.readText() }
        val catalog = CatalogJsonImporter().import(source)

        val mapped = RequirementImportMapper().map(catalog.parts)
        val parents = catalog.parts.associateBy { it.id }
        val items = catalog.parts.filter { it.name == "item" }

        assertEquals(467, items.size)
        assertEquals(386, mapped.size)
        assertEquals(76, items.count { parents[it.parentPartId]?.name == "item" })
        assertEquals(5, items.count { parents[it.parentPartId]?.name == "overview" })
        assertEquals(
            23,
            items.count { parent -> items.any { it.parentPartId == parent.id } },
        )
        assertEquals(mapped.map { it.id }.toSet(), catalog.requirements.map { it.id }.toSet())
        assertTrue(allPartObjects(Json.parseToJsonElement(source).jsonObject["catalog"] as JsonObject)
            .filter { it["name"]?.toString()?.trim('"') == "item" }
            .none { "class" in it })
    }

    private fun allPartObjects(catalog: JsonObject): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        fun visitParts(parent: JsonObject) {
            (parent["parts"] as? JsonArray).orEmpty().forEach { element ->
                val part = element as JsonObject
                result += part
                visitParts(part)
            }
        }
        fun visitControls(parent: JsonObject) {
            (parent["controls"] as? JsonArray).orEmpty().forEach { element ->
                val control = element as JsonObject
                visitParts(control)
                visitControls(control)
            }
        }
        fun visitGroups(groups: JsonArray) {
            groups.forEach { element ->
                val group = element as JsonObject
                visitParts(group)
                visitControls(group)
                (group["groups"] as? JsonArray)?.let(::visitGroups)
            }
        }
        visitGroups(catalog["groups"] as JsonArray)
        return result
    }
}
