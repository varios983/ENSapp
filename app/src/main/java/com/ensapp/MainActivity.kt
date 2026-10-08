package com.ensapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ensapp.data.catalog.CatalogRepositoryImpl
import com.ensapp.data.catalog.DisjunctiveSelectionRepositoryImpl
import com.ensapp.data.response.RequirementResponseRepositoryImpl
import com.ensapp.data.report.PdfReportWriter
import com.ensapp.data.security.FieldCipher
import com.ensapp.data.system.InformationSystemRepositoryImpl
import com.ensapp.data.status.ComplianceStatusRepositoryImpl
import com.ensapp.domain.catalog.BrowseCatalogUseCases
import com.ensapp.domain.report.BuildReportSnapshotUseCase
import com.ensapp.domain.report.CsvReportWriter
import com.ensapp.domain.status.GetComplianceSummaryUseCase
import com.ensapp.domain.catalog.RequirementBrowseItem
import com.ensapp.domain.catalog.RequirementListFilter
import com.ensapp.domain.system.InformationSystem
import com.ensapp.presentation.catalog.CatalogScreen
import com.ensapp.presentation.catalog.CatalogViewModel
import com.ensapp.presentation.catalog.ReinforcementSelectionViewModel
import com.ensapp.presentation.response.RequirementResponseScreen
import com.ensapp.presentation.response.RequirementResponseViewModel
import com.ensapp.presentation.report.CsvExportScreen
import com.ensapp.presentation.report.CsvExportViewModel
import com.ensapp.presentation.report.PdfExportScreen
import com.ensapp.presentation.report.PdfExportViewModel
import com.ensapp.presentation.report.ReportExportFormat
import com.ensapp.presentation.report.ReportExportMenuScreen
import com.ensapp.presentation.status.ComplianceStatusScreen
import com.ensapp.presentation.status.ComplianceViewModel
import com.ensapp.presentation.system.SystemManagementScreen
import com.ensapp.presentation.system.SystemViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = application as EnsApplication
        val database = application.database
        val cipher = FieldCipher()
        val systemRepository = InformationSystemRepositoryImpl(
            database.informationSystemDao(),
            cipher,
        )
        val catalogRepository = CatalogRepositoryImpl(database.catalogDao())
        val responseRepository = RequirementResponseRepositoryImpl(
            dao = database.responseDao(),
            catalogRepository = catalogRepository,
            systemRepository = systemRepository,
            cipher = cipher,
        )
        val selectionRepository = DisjunctiveSelectionRepositoryImpl(
            database.disjunctiveSelectionDao(),
            catalogRepository,
            cipher,
        )
        val complianceRepository = ComplianceStatusRepositoryImpl(
            catalogRepository = catalogRepository,
            systemRepository = systemRepository,
            responseRepository = responseRepository,
            selectionRepository = selectionRepository,
        )
        val browseCatalog = BrowseCatalogUseCases(
            catalogRepository = catalogRepository,
            selectionRepository = selectionRepository,
            responseRepository = responseRepository,
        )
        val reportSnapshot = BuildReportSnapshotUseCase(
            systemRepository = systemRepository,
            browseCatalog = browseCatalog,
            getComplianceSummary = GetComplianceSummaryUseCase(complianceRepository),
        )
        val factory = EnsViewModelFactory(
            systemViewModel = { SystemViewModel(systemRepository) },
            catalogViewModel = {
                CatalogViewModel(browseCatalog)
            },
            responseViewModel = { RequirementResponseViewModel(responseRepository) },
            selectionViewModel = {
                ReinforcementSelectionViewModel(selectionRepository)
            },
            complianceViewModel = { ComplianceViewModel(complianceRepository) },
            pdfExportViewModel = {
                PdfExportViewModel { systemId ->
                    PdfReportWriter().write(reportSnapshot.execute(systemId))
                }
            },
            csvExportViewModel = {
                CsvExportViewModel { systemId ->
                    CsvReportWriter().write(reportSnapshot.execute(systemId))
                }
            },
        )
        val systemViewModel = ViewModelProvider(this, factory)[SystemViewModel::class.java]
        val catalogViewModel = ViewModelProvider(this, factory)[CatalogViewModel::class.java]
        val responseViewModel = ViewModelProvider(this, factory)[RequirementResponseViewModel::class.java]
        val selectionViewModel = ViewModelProvider(this, factory)[ReinforcementSelectionViewModel::class.java]
        val complianceViewModel = ViewModelProvider(this, factory)[ComplianceViewModel::class.java]
        val pdfExportViewModel = ViewModelProvider(this, factory)[PdfExportViewModel::class.java]
        val csvExportViewModel = ViewModelProvider(this, factory)[CsvExportViewModel::class.java]
        setContent {
            MaterialTheme {
                Surface {
                    EnsAppScreen(
                        application = application,
                        systemViewModel = systemViewModel,
                        catalogViewModel = catalogViewModel,
                        responseViewModel = responseViewModel,
                        selectionViewModel = selectionViewModel,
                        complianceViewModel = complianceViewModel,
                        pdfExportViewModel = pdfExportViewModel,
                        csvExportViewModel = csvExportViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnsAppScreen(
    application: EnsApplication,
    systemViewModel: SystemViewModel,
    catalogViewModel: CatalogViewModel,
    responseViewModel: RequirementResponseViewModel,
    selectionViewModel: ReinforcementSelectionViewModel,
    complianceViewModel: ComplianceViewModel,
    pdfExportViewModel: PdfExportViewModel,
    csvExportViewModel: CsvExportViewModel,
) {
    var ready by remember { mutableStateOf(false) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var activeSystem by remember { mutableStateOf<InformationSystem?>(null) }
    var selectedRequirement by remember { mutableStateOf<RequirementBrowseItem?>(null) }
    var showingStatus by remember { mutableStateOf(false) }
    var requirementFilter by remember { mutableStateOf<RequirementListFilter?>(null) }
    var showingExport by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf<ReportExportFormat?>(null) }

    LaunchedEffect(application) {
        try {
            application.awaitCatalogReady()
            ready = true
        } catch (exception: Exception) {
            startupError = exception.message ?: "No se pudo cargar el catálogo ENS"
        }
    }

    when {
        !ready && startupError == null -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text("Preparando el catálogo ENS")
        }
        startupError != null -> Text(
            "Error de inicialización: $startupError",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
        selectedRequirement != null && activeSystem != null -> RequirementResponseScreen(
            viewModel = responseViewModel,
            system = activeSystem!!,
            item = selectedRequirement!!,
            onBack = { selectedRequirement = null },
        )
        showingExport && activeSystem != null && exportFormat == ReportExportFormat.PDF ->
            PdfExportScreen(
                system = activeSystem!!,
                viewModel = pdfExportViewModel,
                onBack = { exportFormat = null; showingExport = false },
            )
        showingExport && activeSystem != null && exportFormat == ReportExportFormat.CSV ->
            CsvExportScreen(
                system = activeSystem!!,
                viewModel = csvExportViewModel,
                onBack = { exportFormat = null; showingExport = false },
            )
        showingExport && activeSystem != null -> ReportExportMenuScreen(
            system = activeSystem!!,
            onChoose = { exportFormat = it },
            onBack = { showingExport = false },
        )
        showingStatus && activeSystem != null -> ComplianceStatusScreen(
            viewModel = complianceViewModel,
            system = activeSystem!!,
            onBack = { showingStatus = false },
            onOpenExport = { showingExport = true; showingStatus = false },
            onOpenRequirements = {
                requirementFilter = it
                showingStatus = false
            },
        )
        activeSystem != null -> CatalogScreen(
            viewModel = catalogViewModel,
            selectionViewModel = selectionViewModel,
            system = activeSystem!!,
            requirementFilter = requirementFilter,
            onOpenRequirement = { selectedRequirement = it },
            onOpenStatus = { showingStatus = true },
            onOpenExport = { showingExport = true },
            onClearFilter = { requirementFilter = null },
            onBackToSystems = {
                selectedRequirement = null
                showingStatus = false
                showingExport = false
                requirementFilter = null
                activeSystem = null
            },
        )
        else -> SystemManagementScreen(
            viewModel = systemViewModel,
            onOpenSystem = {
                requirementFilter = null
                showingStatus = false
                activeSystem = it
            },
        )
    }
}

private class EnsViewModelFactory(
    private val systemViewModel: () -> SystemViewModel,
    private val catalogViewModel: () -> CatalogViewModel,
    private val responseViewModel: () -> RequirementResponseViewModel,
    private val selectionViewModel: () -> ReinforcementSelectionViewModel,
    private val complianceViewModel: () -> ComplianceViewModel,
    private val pdfExportViewModel: () -> PdfExportViewModel,
    private val csvExportViewModel: () -> CsvExportViewModel,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T = when {
        modelClass.isAssignableFrom(SystemViewModel::class.java) -> systemViewModel()
        modelClass.isAssignableFrom(CatalogViewModel::class.java) -> catalogViewModel()
        modelClass.isAssignableFrom(RequirementResponseViewModel::class.java) ->
            responseViewModel()
        modelClass.isAssignableFrom(ReinforcementSelectionViewModel::class.java) ->
            selectionViewModel()
        modelClass.isAssignableFrom(ComplianceViewModel::class.java) -> complianceViewModel()
        modelClass.isAssignableFrom(PdfExportViewModel::class.java) -> pdfExportViewModel()
        modelClass.isAssignableFrom(CsvExportViewModel::class.java) -> csvExportViewModel()
        else -> throw IllegalArgumentException("ViewModel no soportado: ${modelClass.name}")
    } as T
}
