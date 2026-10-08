package com.ensapp.data.response

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ensapp.data.catalog.CatalogRepositoryImpl
import com.ensapp.data.local.EnsDatabase
import com.ensapp.data.security.FieldCipher
import com.ensapp.data.system.InformationSystemRepositoryImpl
import com.ensapp.domain.catalog.ApplicabilityCriterion
import com.ensapp.domain.catalog.ApplicabilityRule
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogProperty
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.SecurityDimension
import com.ensapp.domain.catalog.Catalog
import com.ensapp.domain.system.InformationSystem
import com.ensapp.domain.response.MaturityLevel
import com.ensapp.domain.response.SaveRequirementResponseUseCase
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequirementResponsePersistenceTest {
    private lateinit var database: EnsDatabase
    private lateinit var cipher: FieldCipher

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EnsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cipher = FieldCipher("response-test-${UUID.randomUUID()}")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun responsePersistsAsExplicitL0AndRemainsIsolatedBySystem() = runBlocking {
        database.catalogDao().import(testCatalog())
        val systemRepository = InformationSystemRepositoryImpl(
            database.informationSystemDao(),
            cipher,
        )
        val responseRepository = RequirementResponseRepositoryImpl(
            dao = database.responseDao(),
            catalogRepository = CatalogRepositoryImpl(database.catalogDao()),
            systemRepository = systemRepository,
            cipher = cipher,
        )
        val firstSystem = system("system-1")
        val secondSystem = system("system-2")
        systemRepository.save(firstSystem)
        systemRepository.save(secondSystem)

        assertNull(responseRepository.get(firstSystem.id, "req-1"))
        SaveRequirementResponseUseCase(responseRepository).execute(
            firstSystem.id,
            "req-1",
            MaturityLevel.L0,
            "Explicitamente inexistente",
        )

        val restored = responseRepository.get(firstSystem.id, "req-1")
        assertEquals(MaturityLevel.L0, restored?.maturity)
        assertEquals("Explicitamente inexistente", restored?.note)
        assertNull(responseRepository.get(secondSystem.id, "req-1"))
    }

    private fun system(id: String) = InformationSystem(
        id = id,
        name = id,
        category = EnsCategory.BASICA,
        dimensions = SecurityDimension.entries.associateWith { DimensionLevel.BAJO },
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun testCatalog() = Catalog(
        revision = CatalogRevision("test-revision", "test-uuid", "test", "1.1.3", null, "checksum"),
        frameworks = listOf(Framework("org", "Organizativo", null, 0, emptyList())),
        families = emptyList(),
        controls = listOf(
            CatalogControl(
                id = "org.1",
                kind = ControlKind.MEASURE,
                parentControlId = null,
                frameworkId = "org",
                familyId = null,
                title = "Medida",
                controlClass = null,
                label = "org.1",
                sortOrder = 0,
                properties = emptyList<CatalogProperty>(),
                applicability = ApplicabilityRule(
                    criterion = ApplicabilityCriterion.CATEGORY,
                    categories = setOf(EnsCategory.BASICA),
                ),
            ),
        ),
        parameters = emptyList(),
        parts = listOf(
            CatalogPart(
                id = "requirements-1",
                ownerControlId = "org.1",
                ownerGroupId = null,
                parentPartId = null,
                name = "requisitos",
                namespace = null,
                label = null,
                prose = null,
                sortOrder = 0,
                properties = emptyList(),
            ),
            CatalogPart(
                id = "req-1",
                ownerControlId = "org.1",
                ownerGroupId = null,
                parentPartId = "requirements-1",
                name = "item",
                namespace = null,
                label = "R1",
                prose = "Requisito de prueba",
                sortOrder = 0,
                properties = emptyList(),
            ),
        ),
        requirements = listOf(
            com.ensapp.domain.catalog.Requirement(
                id = "req-1",
                controlId = "org.1",
                label = "R1",
                text = "Requisito de prueba",
                sortOrder = 0,
            ),
        ),
    )
}
