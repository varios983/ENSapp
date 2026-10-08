package com.ensapp.domain.response

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class SaveRequirementResponseUseCaseTest {
    @Test
    fun `saves explicit L0 and note only for a respondible applicable requirement`() =
        runBlocking {
            val repository = Mockito.mock(RequirementResponseRepository::class.java)
            Mockito.`when`(repository.isRespondibleAndApplicable("system-1", "req-1"))
                .thenReturn(true)

            val result = SaveRequirementResponseUseCase(repository).execute(
                systemId = "system-1",
                requirementId = "req-1",
                maturity = MaturityLevel.L0,
                note = "Evidencia de prueba",
            )

            assertEquals(MaturityLevel.L0, result.maturity)
            assertEquals("Evidencia de prueba", result.note)
            Mockito.verify(repository).save(result)
        }

    @Test
    fun `missing response stays distinct from L0`() = runBlocking {
        val repository = Mockito.mock(RequirementResponseRepository::class.java)
        Mockito.`when`(repository.get("system-1", "req-1")).thenReturn(null)
        assertNull(repository.get("system-1", "req-1"))

        Mockito.`when`(repository.isRespondibleAndApplicable("system-1", "req-1"))
            .thenReturn(true)
        val saved = SaveRequirementResponseUseCase(repository).execute(
            "system-1",
            "req-1",
            MaturityLevel.L0,
            null,
        )
        assertEquals("system-1", saved.systemId)
        assertEquals("req-1", saved.requirementId)
        assertEquals(MaturityLevel.L0, saved.maturity)
        Mockito.verify(repository).save(saved)
    }

    @Test
    fun `rejects nonapplicable or nonrespondible requirement`() {
        val repository = Mockito.mock(RequirementResponseRepository::class.java)
        runBlocking {
            Mockito.`when`(repository.isRespondibleAndApplicable("system-1", "req-1"))
                .thenReturn(false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SaveRequirementResponseUseCase(repository).execute(
                    "system-1",
                    "req-1",
                    MaturityLevel.L1,
                    null,
                )
            }
        }
        runBlocking {
            Mockito.verify(repository).isRespondibleAndApplicable("system-1", "req-1")
        }
        Mockito.verifyNoMoreInteractions(repository)
    }

    @Test
    fun `records response modification time`() = runBlocking {
        val repository = Mockito.mock(RequirementResponseRepository::class.java)
        Mockito.`when`(repository.isRespondibleAndApplicable("s", "r")).thenReturn(true)

        val response = SaveRequirementResponseUseCase(repository).execute(
            "s",
            "r",
            MaturityLevel.L5,
            null,
        )

        assertEquals(true, response.updatedAt.isAfter(Instant.EPOCH))
    }
}
