package com.ensapp.domain.catalog

class SelectReinforcementUseCase(
    private val repository: DisjunctiveSelectionRepository,
) {
    suspend fun execute(
        systemId: String,
        parameter: CatalogParameter,
        reinforcementId: String,
    ): DisjunctiveSelection {
        require(systemId.isNotBlank()) { "Falta el sistema de información" }
        require(parameter.type == "seleccion-refuerzo") {
            "El parámetro no declara una selección disyuntiva"
        }
        require(parameter.selectionCardinality == "one") {
            "La selección del parámetro debe tener cardinalidad one"
        }
        require(parameter.choices.isNotEmpty()) { "El parámetro no declara opciones" }
        require(reinforcementId in parameter.choices) {
            "El refuerzo no pertenece a las opciones declaradas por el parámetro"
        }

        val selection = DisjunctiveSelection(
            systemId = systemId,
            parameterId = parameter.id,
            reinforcementId = reinforcementId,
        )
        repository.save(selection)
        return selection
    }
}
