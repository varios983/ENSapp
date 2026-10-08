package com.ensapp.domain.catalog

import com.ensapp.domain.system.InformationSystem
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicabilityCalculatorTest {
    private val calculator = ApplicabilityCalculator()

    @Test
    fun `matches each category only for category-based controls`() {
        EnsCategory.entries.forEach { category ->
            val system = system(category = category)
            val control = control(
                rule = ApplicabilityRule(
                    criterion = ApplicabilityCriterion.CATEGORY,
                    categories = setOf(category),
                ),
            )

            assertEquals(ApplicabilityStatus.APPLIES, calculator.evaluate(control, system).status)
            assertEquals(
                ApplicabilityStatus.DOES_NOT_APPLY,
                calculator.evaluate(control, system(category = otherCategory(category))).status,
            )
        }
    }

    @Test
    fun `matches a configured dimension level independently of category`() {
        SecurityDimension.entries.forEach { dimension ->
            val configured = system(
                category = EnsCategory.BASICA,
                levels = SecurityDimension.entries.associateWith { DimensionLevel.BAJO }
                    .toMutableMap()
                    .apply { put(dimension, DimensionLevel.ALTO) },
            )
            val control = control(
                rule = ApplicabilityRule(
                    criterion = ApplicabilityCriterion.DIMENSION_LEVEL,
                    dimensions = setOf(dimension),
                    levels = setOf(DimensionLevel.ALTO),
                ),
            )

            assertEquals(ApplicabilityStatus.APPLIES, calculator.evaluate(control, configured).status)
            assertEquals(
                ApplicabilityStatus.DOES_NOT_APPLY,
                calculator.evaluate(
                    control,
                    system(category = EnsCategory.ALTA),
                ).status,
            )
        }
    }

    @Test
    fun `a reinforcement without its own rule inherits its parent rule and provenance`() {
        val measure = control(
            id = "op.acc.1",
            rule = ApplicabilityRule(
                criterion = ApplicabilityCriterion.CATEGORY,
                categories = setOf(EnsCategory.BASICA),
            ),
        )
        val reinforcement = control(
            id = "op.acc.1.r1",
            kind = ControlKind.REINFORCEMENT,
            parentId = measure.id,
            rule = ApplicabilityRule(ApplicabilityCriterion.UNSPECIFIED),
        )

        val result = calculator.evaluate(
            reinforcement,
            system(EnsCategory.BASICA),
            mapOf(measure.id to measure),
        )

        assertEquals(ApplicabilityStatus.APPLIES, result.status)
        assertEquals(ApplicabilityProvenance.INHERITED_FROM_MEASURE, result.provenance)
    }

    @Test
    fun `a reinforcement own rule takes precedence over parent applicability`() {
        val measure = control(
            id = "op.acc.1",
            rule = ApplicabilityRule(
                criterion = ApplicabilityCriterion.CATEGORY,
                categories = setOf(EnsCategory.BASICA),
            ),
        )
        val reinforcement = control(
            id = "op.acc.1.r1",
            kind = ControlKind.REINFORCEMENT,
            parentId = measure.id,
            rule = ApplicabilityRule(
                criterion = ApplicabilityCriterion.CATEGORY,
                categories = setOf(EnsCategory.ALTA),
            ),
        )

        val result = calculator.evaluate(
            reinforcement,
            system(EnsCategory.BASICA),
            mapOf(measure.id to measure),
        )

        assertEquals(ApplicabilityStatus.DOES_NOT_APPLY, result.status)
        assertEquals(ApplicabilityProvenance.OWN, result.provenance)
    }

    @Test
    fun `incomplete applicability metadata requires review`() {
        val control = control(
            rule = ApplicabilityRule(
                criterion = ApplicabilityCriterion.DIMENSION_LEVEL,
                dimensions = setOf(SecurityDimension.CONFIDENCIALIDAD),
            ),
        )

        assertEquals(
            ApplicabilityStatus.REVIEW,
            calculator.evaluate(control, system(EnsCategory.BASICA)).status,
        )
    }

    @Test
    fun `matches any declared dimension without pairing unrelated level lists`() {
        val control = control(
            rule = ApplicabilityRule(
                criterion = ApplicabilityCriterion.DIMENSION_LEVEL,
                dimensions = setOf(
                    SecurityDimension.CONFIDENCIALIDAD,
                    SecurityDimension.INTEGRIDAD,
                ),
                levels = setOf(DimensionLevel.BAJO, DimensionLevel.ALTO),
            ),
        )
        val system = system(
            category = EnsCategory.MEDIA,
            levels = SecurityDimension.entries.associateWith { DimensionLevel.MEDIO }
                .toMutableMap()
                .apply { put(SecurityDimension.CONFIDENCIALIDAD, DimensionLevel.ALTO) },
        )

        assertEquals(ApplicabilityStatus.APPLIES, calculator.evaluate(control, system).status)
        assertEquals(
            ApplicabilityStatus.DOES_NOT_APPLY,
            calculator.evaluate(
                control,
                system(
                    category = EnsCategory.MEDIA,
                    levels = SecurityDimension.entries.associateWith { DimensionLevel.MEDIO },
                ),
            ).status,
        )
    }

    private fun system(
        category: EnsCategory,
        levels: Map<SecurityDimension, DimensionLevel> =
            SecurityDimension.entries.associateWith { DimensionLevel.MEDIO },
    ) = InformationSystem(
        id = "system-1",
        name = "Sistema",
        category = category,
        dimensions = levels,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun control(
        id: String = "op.acc.1",
        kind: ControlKind = ControlKind.MEASURE,
        parentId: String? = null,
        rule: ApplicabilityRule,
    ) = CatalogControl(
        id = id,
        kind = kind,
        parentControlId = parentId,
        frameworkId = "op",
        familyId = "op.acc",
        title = id,
        controlClass = null,
        label = id,
        sortOrder = 0,
        properties = emptyList(),
        applicability = rule,
    )

    private fun otherCategory(category: EnsCategory): EnsCategory =
        EnsCategory.entries.first { it != category }
}
