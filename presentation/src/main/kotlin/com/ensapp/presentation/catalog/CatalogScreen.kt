package com.ensapp.presentation.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensapp.domain.catalog.ApplicabilityEvaluation
import com.ensapp.domain.catalog.ApplicabilityStatus
import com.ensapp.domain.catalog.CatalogPartNode
import com.ensapp.domain.catalog.ControlBrowseNode
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.FamilyBrowseNode
import com.ensapp.domain.catalog.FrameworkBrowseNode
import com.ensapp.domain.catalog.RequirementListFilter
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.system.InformationSystem

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    selectionViewModel: ReinforcementSelectionViewModel,
    system: InformationSystem,
    requirementFilter: RequirementListFilter?,
    onOpenRequirement: (RequirementBrowseItem) -> Unit,
    onOpenStatus: () -> Unit,
    onOpenExport: () -> Unit,
    onClearFilter: () -> Unit,
    onBackToSystems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.state.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }
    var framework by remember { mutableStateOf<FrameworkBrowseNode?>(null) }
    var family by remember { mutableStateOf<FamilyBrowseNode?>(null) }
    var control by remember { mutableStateOf<ControlBrowseNode?>(null) }

    LaunchedEffect(system.id, system.updatedAt) {
        framework = null
        family = null
        control = null
        viewModel.load(system)
    }
    LaunchedEffect(system.id, selectionState.selections) {
        if (selectionState.selections.isNotEmpty()) viewModel.load(system)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text("Catálogo ENS", style = MaterialTheme.typography.headlineSmall)
            Text(system.name, style = MaterialTheme.typography.titleMedium)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (requirementFilter != null) {
                OutlinedButton(onClick = onClearFilter) { Text("Quitar filtro") }
            } else {
                OutlinedButton(onClick = onOpenStatus) { Text("Estado") }
            }
            OutlinedButton(onClick = onOpenExport) { Text("Exportar") }
            OutlinedButton(onClick = onBackToSystems) { Text("Sistemas") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar también N/A")
            Switch(
                checked = showAll,
                onCheckedChange = { showAll = it },
                modifier = Modifier.semantics {
                    contentDescription = "Mostrar elementos no aplicables"
                },
            )
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
            state.tree != null -> {
                val tree = state.tree!!
                when {
                    requirementFilter != null -> FilteredRequirements(
                        tree = tree,
                        filter = requirementFilter,
                        category = system.category,
                        onOpenRequirement = onOpenRequirement,
                    )
                    control != null -> ControlDetail(
                        node = control!!,
                        category = system.category,
                        systemId = system.id,
                        selectionViewModel = selectionViewModel,
                        showAll = showAll,
                        requirementFilter = requirementFilter,
                        onBack = { control = null },
                        onSelectControl = { control = it },
                        onOpenRequirement = onOpenRequirement,
                    )
                    family != null -> FamilyDetail(
                        node = family!!,
                        category = system.category,
                        showAll = showAll,
                        onBack = { family = null },
                        onSelectControl = { control = it },
                    )
                    framework != null -> FrameworkDetail(
                        node = framework!!,
                        category = system.category,
                        showAll = showAll,
                        onBack = { framework = null },
                        onSelectFamily = { family = it },
                        onSelectControl = { control = it },
                    )
                    else -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        tree.frameworks
                            .filter {
                                showAll ||
                                    it.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY
                            }
                            .forEach { node ->
                                BrowseCard(
                                    title = node.framework.label ?: node.framework.title,
                                    subtitle = node.framework.id,
                                    evaluation = node.applicability,
                                    category = system.category,
                                    onOpen = { framework = node },
                                    enabled = node.applicability.status !=
                                        ApplicabilityStatus.DOES_NOT_APPLY,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FilteredRequirements(
    tree: com.ensapp.domain.catalog.CatalogBrowseTree,
    filter: RequirementListFilter,
    category: EnsCategory,
    onOpenRequirement: (RequirementBrowseItem) -> Unit,
) {
    val controls = tree.frameworks.flatMap { framework ->
        framework.controls + framework.families.flatMap { it.controls }
    }
    val allControls = buildList {
        fun addTree(node: ControlBrowseNode) {
            add(node)
            node.children.forEach { addTree(it) }
        }
        controls.forEach { addTree(it) }
    }
    val requirements = allControls.flatMap { node ->
        node.requirements.filter { item ->
            item.applicability.status == ApplicabilityStatus.APPLIES &&
                (filter.dimension == null || filter.dimension in item.dimensions) &&
                when (filter.status) {
                    com.ensapp.domain.catalog.RequirementFilterStatus.PENDING ->
                        item.response == null
                    com.ensapp.domain.catalog.RequirementFilterStatus.ANSWERED ->
                        item.response != null
                }
        }.map { node.control to it }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (requirements.isEmpty()) {
            Text("No hay requisitos que coincidan con este filtro.")
        }
        requirements.forEach { (control, item) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${control.label ?: control.id} · ${control.title}")
                    Text(
                        item.requirement.label ?: item.requirement.id,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    ApplicabilityBadge(item.applicability, category)
                    item.content.part.prose?.let { Text(it) }
                    Button(onClick = { onOpenRequirement(item) }) { Text("Abrir requisito") }
                }
            }
        }
    }
}

@Composable
private fun FrameworkDetail(
    node: FrameworkBrowseNode,
    category: EnsCategory,
    showAll: Boolean,
    onBack: () -> Unit,
    onSelectFamily: (FamilyBrowseNode) -> Unit,
    onSelectControl: (ControlBrowseNode) -> Unit,
) {
    BrowseHeading(node.framework.title, node.applicability, category, onBack)
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
        node.families
            .filter { showAll || it.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY }
            .forEach { family ->
                BrowseCard(
                    title = family.family.label ?: family.family.title,
                    subtitle = family.family.id,
                    evaluation = family.applicability,
                    category = category,
                    onOpen = { onSelectFamily(family) },
                    enabled = family.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY,
                )
            }
        node.controls
            .filter { showAll || it.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY }
            .forEach { control ->
                ControlCard(control, category, showAll, onSelectControl)
            }
    }
}

@Composable
private fun FamilyDetail(
    node: FamilyBrowseNode,
    category: EnsCategory,
    showAll: Boolean,
    onBack: () -> Unit,
    onSelectControl: (ControlBrowseNode) -> Unit,
) {
    BrowseHeading(node.family.title, node.applicability, category, onBack)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        node.controls
            .filter { showAll || it.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY }
            .forEach { ControlCard(it, category, showAll, onSelectControl) }
    }
}

@Composable
private fun ControlDetail(
    node: ControlBrowseNode,
    category: EnsCategory,
    systemId: String,
    selectionViewModel: ReinforcementSelectionViewModel,
    showAll: Boolean,
    requirementFilter: RequirementListFilter?,
    onBack: () -> Unit,
    onSelectControl: (ControlBrowseNode) -> Unit,
    onOpenRequirement: (RequirementBrowseItem) -> Unit,
) {
    BrowseHeading(node.control.title, node.applicability, category, onBack)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        ReinforcementSelection(
            viewModel = selectionViewModel,
            systemId = systemId,
            parameters = node.parameters,
            enabled = node.applicability.status == ApplicabilityStatus.APPLIES,
        )
        node.children
            .filter { showAll || it.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY }
            .forEach { ControlCard(it, category, showAll, onSelectControl) }
        node.requirements
            .filter { item ->
                requirementFilter == null ||
                    (item.applicability.status == ApplicabilityStatus.APPLIES &&
                        (requirementFilter.dimension == null ||
                            requirementFilter.dimension in item.dimensions) &&
                        when (requirementFilter.status) {
                            com.ensapp.domain.catalog.RequirementFilterStatus.PENDING ->
                                item.response == null
                            com.ensapp.domain.catalog.RequirementFilterStatus.ANSWERED ->
                                item.response != null
                        })
            }
            .filter { showAll || it.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY }
            .forEach { requirement ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            requirement.requirement.label ?: requirement.requirement.id,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        ApplicabilityBadge(requirement.applicability, category)
                        requirement.content.part.prose?.let { Text(it) }
                        requirement.content.children.forEach { PartContent(it) }
                        Button(
                            onClick = { onOpenRequirement(requirement) },
                            enabled = requirement.applicability.status == ApplicabilityStatus.APPLIES,
                        ) {
                            Text(
                                if (requirement.applicability.status == ApplicabilityStatus.APPLIES) {
                                    "Responder"
                                } else {
                                    "No respondible"
                                },
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun PartContent(node: CatalogPartNode) {
    Column(Modifier.padding(start = 12.dp, top = 4.dp)) {
        node.part.prose?.let { Text(it) }
        node.children.forEach { PartContent(it) }
    }
}

@Composable
private fun ControlCard(
    node: ControlBrowseNode,
    category: EnsCategory,
    showAll: Boolean,
    onSelect: (ControlBrowseNode) -> Unit,
) {
    BrowseCard(
        title = node.control.label ?: node.control.title,
        subtitle = node.control.id,
        evaluation = node.applicability,
        category = category,
        onOpen = { onSelect(node) },
        enabled = showAll || node.applicability.status != ApplicabilityStatus.DOES_NOT_APPLY,
    )
}

@Composable
private fun BrowseHeading(
    title: String,
    evaluation: ApplicabilityEvaluation,
    category: EnsCategory,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onBack) { Text("Atrás") }
        Spacer(Modifier.padding(horizontal = 4.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            ApplicabilityBadge(evaluation, category)
        }
    }
}

@Composable
private fun BrowseCard(
    title: String,
    subtitle: String,
    evaluation: ApplicabilityEvaluation,
    category: EnsCategory,
    onOpen: () -> Unit,
    enabled: Boolean,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                ApplicabilityBadge(evaluation, category)
            }
            Button(onClick = onOpen, enabled = enabled) { Text("Abrir") }
        }
    }
}

@Composable
internal fun ApplicabilityBadge(
    evaluation: ApplicabilityEvaluation,
    category: EnsCategory,
) {
    val label = when (evaluation.status) {
        ApplicabilityStatus.APPLIES -> "Aplica · ${category.name}"
        ApplicabilityStatus.DOES_NOT_APPLY -> "No aplica"
        ApplicabilityStatus.REVIEW -> "Revisión necesaria"
    }
    val color = when (evaluation.status) {
        ApplicabilityStatus.DOES_NOT_APPLY -> Color(0xFF757575)
        ApplicabilityStatus.REVIEW -> Color(0xFF1565C0)
        ApplicabilityStatus.APPLIES -> when (category) {
            EnsCategory.BASICA -> Color(0xFF2E7D32)
            EnsCategory.MEDIA -> Color(0xFFF9A825)
            EnsCategory.ALTA -> Color(0xFFC62828)
        }
    }
    val semanticLabel = when (evaluation.status) {
        ApplicabilityStatus.DOES_NOT_APPLY -> "No aplica, gris"
        ApplicabilityStatus.REVIEW -> "Revisión necesaria, azul"
        ApplicabilityStatus.APPLIES -> when (category) {
            EnsCategory.BASICA -> "Aplica, categoría básica, verde"
            EnsCategory.MEDIA -> "Aplica, categoría media, amarillo"
            EnsCategory.ALTA -> "Aplica, categoría alta, rojo"
        }
    }
    Text(
        text = "● $label",
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics {
            contentDescription = semanticLabel
            stateDescription = label
        },
    )
}
