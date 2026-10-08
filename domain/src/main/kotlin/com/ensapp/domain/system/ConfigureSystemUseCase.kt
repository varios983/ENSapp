package com.ensapp.domain.system

class ConfigureSystemUseCase(
    private val repository: InformationSystemRepository,
) {
    suspend fun execute(system: InformationSystem): InformationSystem {
        repository.save(system)
        return system
    }
}
