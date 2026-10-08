package com.ensapp.domain.system

import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.SecurityDimension
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ConfigureSystemUseCaseTest {
    @org.mockito.Mock
    private lateinit var repository: InformationSystemRepository

    @Test
    fun `persists the system with all five configured dimensions`() =
        kotlinx.coroutines.runBlocking {
            val system = InformationSystem(
                id = "system-1",
                name = "Sistema",
                category = EnsCategory.MEDIA,
                dimensions = SecurityDimension.entries.associateWith { DimensionLevel.BAJO },
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            )

            val saved = ConfigureSystemUseCase(repository).execute(system)

            assertEquals(system, saved)
            verify(repository).save(system)
        }
}
