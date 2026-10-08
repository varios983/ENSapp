package com.ensapp.domain.catalog

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class SelectReinforcementUseCaseTest {
    private val groups = listOf(
        "op.acc.5" to 3,
        "op.acc.6" to 3,
        "mp.com.4" to 2,
        "mp.s.2" to 2,
    ).flatMap { (controlId, count) ->
        (1..count).map { order ->
            CatalogParameter(
                id = "$controlId.parameter.$order",
                controlId = controlId,
                label = null,
                usage = null,
                type = "seleccion-refuerzo",
                scopeKind = null,
                scopeValue = null,
                selectionCardinality = "one",
                sortOrder = order,
                properties = emptyList(),
                choices = listOf("$controlId.r1", "$controlId.r2"),
            )
        }
    }

    @Test
    fun `selects only a declared choice from each single-choice group`() = runBlocking {
        val repository = Mockito.mock(DisjunctiveSelectionRepository::class.java)
        val useCase = SelectReinforcementUseCase(repository)

        groups.forEach { parameter ->
            val result = useCase.execute("system-1", parameter, parameter.choices.first())
            assertEquals(parameter.id, result.parameterId)
            assertEquals(parameter.choices.first(), result.reinforcementId)
            Mockito.verify(repository).save(result)
        }
        assertEquals(10, groups.size)
    }

    @Test
    fun `rejects a reinforcement not present in choice`() {
        val repository = Mockito.mock(DisjunctiveSelectionRepository::class.java)
        val parameter = groups.first()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SelectReinforcementUseCase(repository).execute(
                    "system-1",
                    parameter,
                    "op.acc.5.r999",
                )
            }
        }
        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `allows explicit alternative change and isolates system selections`() = runBlocking {
        val repository = Mockito.mock(DisjunctiveSelectionRepository::class.java)
        val parameter = groups.first()
        val useCase = SelectReinforcementUseCase(repository)
        val first = useCase.execute("system-1", parameter, parameter.choices[0])
        val changed = useCase.execute("system-1", parameter, parameter.choices[1])
        val otherSystem = useCase.execute("system-2", parameter, parameter.choices[0])

        assertEquals(first.parameterId, changed.parameterId)
        assertEquals(parameter.choices[1], changed.reinforcementId)
        assertEquals("system-2", otherSystem.systemId)
        Mockito.verify(repository).save(first)
        Mockito.verify(repository).save(changed)
        Mockito.verify(repository).save(otherSystem)
    }

    @Test
    fun `rejects parameters that do not declare cardinality one`() {
        val repository = Mockito.mock(DisjunctiveSelectionRepository::class.java)
        val parameter = groups.first().copy(selectionCardinality = "any")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SelectReinforcementUseCase(repository).execute(
                    "system-1",
                    parameter,
                    parameter.choices.first(),
                )
            }
        }
        Mockito.verifyNoInteractions(repository)
    }
}
