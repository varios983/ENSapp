package com.ensapp.domain.system

interface InformationSystemRepository {
    suspend fun save(system: InformationSystem)
    suspend fun get(id: String): InformationSystem?
    suspend fun list(): List<InformationSystem>
}
