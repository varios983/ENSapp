package com.ensapp.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ensapp.domain.catalog.ApplicabilityEvaluation
import com.ensapp.domain.catalog.ApplicabilityProvenance
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.CatalogParameter
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogPartNode
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.DisjunctiveSelection
import com.ensapp.domain.catalog.DisjunctiveSelectionRepository
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.Requirement
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.response.RequirementResponse
import com.ensapp.domain.response.RequirementResponseRepository
import com.ensapp.domain.system.InformationSystem
import com.ensapp.presentation.catalog.ApplicabilityBadge
import com.ensapp.presentation.catalog.ReinforcementSelection
import com.ensapp.presentation.catalog.ReinforcementSelectionViewModel
import com.ensapp.presentation.response.RequirementResponseScreen
import com.ensapp.presentation.response.RequirementResponseViewModel
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun applicability_is_announced_as_text_and_not_only_as_color() {
        composeRule.setContent {
            Column {
                ApplicabilityBadge(evaluation(ApplicabilityStatus.DOES_NOT_APPLY), EnsCategory.BASICA)
                ApplicabilityBadge(evaluation(ApplicabilityStatus.APPLIES), EnsCategory.MEDIA)
                ApplicabilityBadge(evaluation(ApplicabilityStatus.REVIEW), EnsCategory.ALTA)
            }
        }

        composeRule.onNodeWithContentDescription("No aplica, gris").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Aplica, categoría media, amarillo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Revisión necesaria, azul").assertIsDisplayed()
        composeRule.onNodeWithText("● No aplica").assertIsDisplayed()
        composeRule.onNodeWithText("● Aplica · MEDIA").assertIsDisplayed()
    }

    @Test
    fun response_and_reinforcement_choices_have_labeled_radio_actions() {
        val system = InformationSystem(
            "system-1",
            "Sistema",
            EnsCategory.MEDIA,
            SecurityDimension.entries.associateWith { DimensionLevel.MEDIO },
            Instant.EPOCH,
            Instant.EPOCH,
        )
        val requirement = RequirementBrowseItem(
            Requirement("req-1", "op.acc.1", "R1", "Texto", 0),
            evaluation(ApplicabilityStatus.APPLIES),
            emptySet(),
            null,
            CatalogPartNode(
                CatalogPart(
                    "req-1",
                    "op.acc.1",
                    null,
                    null,
                    "item",
                    null,
                    "R1",
                    "Texto",
                    0,
                    emptyList(),
                ),
                emptyList(),
            ),
        )
        val responseViewModel = RequirementResponseViewModel(FakeResponseRepository())
        composeRule.setContent {
            RequirementResponseScreen(
                viewModel = responseViewModel,
                system = system,
                item = requirement,
                onBack = {},
            )
        }
        composeRule.onNodeWithText("L0 · Inexistente")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("Guardar respuesta").assertHasClickAction()
    }

    @Test
    fun reinforcement_choices_are_labeled_radio_actions() {
        val systemId = "system-1"
        val parameter = CatalogParameter(
            id = "parameter-1",
            controlId = "op.acc.5",
            label = "Alternativa disyuntiva",
            usage = null,
            type = "seleccion-refuerzo",
            scopeKind = null,
            scopeValue = null,
            selectionCardinality = "one",
            sortOrder = 0,
            properties = emptyList(),
            choices = listOf("op.acc.5.r1", "op.acc.5.r2"),
        )
        val selectionViewModel = ReinforcementSelectionViewModel(FakeSelectionRepository())
        composeRule.setContent {
            ReinforcementSelection(
                viewModel = selectionViewModel,
                systemId = systemId,
                parameters = listOf(parameter),
                enabled = true,
            )
        }
        composeRule.onNodeWithText("op.acc.5.r1")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
            .assertIsSelected()
    }

    private fun evaluation(status: ApplicabilityStatus) = ApplicabilityEvaluation(
        status,
        null,
        ApplicabilityProvenance.OWN,
    )

    private class FakeResponseRepository : RequirementResponseRepository {
        override suspend fun get(systemId: String, requirementId: String): RequirementResponse? = null
        override suspend fun list(systemId: String): List<RequirementResponse> = emptyList()
        override suspend fun save(response: RequirementResponse) = Unit
        override suspend fun isRespondibleAndApplicable(
            systemId: String,
            requirementId: String,
        ) = true
    }

    private class FakeSelectionRepository : DisjunctiveSelectionRepository {
        private var selection: DisjunctiveSelection? = null

        override suspend fun selected(systemId: String, parameterId: String) =
            selection?.takeIf { it.systemId == systemId && it.parameterId == parameterId }

        override suspend fun save(selection: DisjunctiveSelection) {
            this.selection = selection
        }
    }
}
