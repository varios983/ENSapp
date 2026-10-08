package com.ensapp.data.catalog

import java.nio.file.Files
import java.nio.file.Path
import com.ensapp.domain.catalog.ApplicabilityCriterion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CatalogRevisionRegressionTest {
    @Test
    fun `rev10 counts hierarchy parameters and inherited reinforcement set stay fixed`() {
        val source = Files.newBufferedReader(Path.of("..", "datos", "ENS_Anexo_II_rev_10.json"))
            .use { it.readText() }
        val catalog = CatalogJsonImporter().import(source)
        val partsById = catalog.parts.associateBy { it.id }
        val itemParts = catalog.parts.filter { it.name == "item" }
        val inheritedIds = catalog.controls
            .filter {
                it.kind.name == "REINFORCEMENT" &&
                    it.applicability?.criterion == ApplicabilityCriterion.UNSPECIFIED
            }
            .map { it.id }
            .toSet()

        assertEquals(3, catalog.frameworks.size)
        assertEquals(15, catalog.families.size)
        assertEquals(73, catalog.controls.count { it.kind.name == "MEASURE" })
        assertEquals(133, catalog.controls.count { it.kind.name == "REINFORCEMENT" })
        assertEquals(386, catalog.requirements.size)
        assertEquals(467, itemParts.size)
        assertEquals(
            4,
            catalog.controls.count {
                it.frameworkId == "org" && it.familyId == null && it.parentControlId == null
            },
        )
        assertEquals(10, catalog.parameters.count { it.selectionCardinality == "one" })
        assertEquals(
            setOf("op.acc.5", "op.acc.6", "mp.com.4", "mp.s.2"),
            catalog.parameters.map { it.controlId }.toSet(),
        )
        assertEquals(41, inheritedIds.size)
        assertEquals(
            setOf(
                "org.2.r1", "org.3.r1", "op.pl.5.r1", "op.pl.5.r2", "op.acc.2.r2",
                "op.acc.3.r2", "op.acc.3.r3", "op.acc.5.r6", "op.acc.5.r7",
                "op.exp.1.r1", "op.exp.1.r2", "op.exp.1.r3", "op.exp.1.r4",
                "op.exp.3.r4", "op.exp.3.r5", "op.exp.4.r3", "op.exp.4.r4",
                "op.exp.6.r5", "op.exp.7.r4", "op.exp.10.r2", "op.ext.3.r1",
                "op.ext.3.r2", "op.ext.3.r3", "op.cont.2.r1", "op.cont.2.r2",
                "op.cont.4.r1", "op.mon.1.r3", "op.mon.3.r7", "mp.per.1.r1",
                "mp.eq.4.r2", "mp.com.2.r4", "mp.com.2.r5", "mp.com.3.r5",
                "mp.si.1.r1", "mp.si.5.r2", "mp.sw.1.r5", "mp.sw.2.r2",
                "mp.info.3.r5", "mp.info.4.r1", "mp.s.3.r2", "mp.s.4.r2",
            ),
            inheritedIds,
        )
        assertEquals(386, itemParts.count { partsById[it.parentPartId]?.name == "requisitos" })
        assertEquals(76, itemParts.count { partsById[it.parentPartId]?.name == "item" })
        assertEquals(5, itemParts.count { partsById[it.parentPartId]?.name == "overview" })
        assertNull(catalog.controls.first { it.id == "org.1" }.familyId)
    }
}
