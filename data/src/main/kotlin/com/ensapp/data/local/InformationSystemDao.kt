package com.ensapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface InformationSystemDao {
    @Upsert
    suspend fun upsertSystem(system: InformationSystemEntity)

    @Upsert
    suspend fun upsertDimensions(dimensions: List<SystemDimensionEntity>)

    @Transaction
    suspend fun upsertSystemWithDimensions(
        system: InformationSystemEntity,
        dimensions: List<SystemDimensionEntity>,
    ) {
        upsertSystem(system)
        upsertDimensions(dimensions)
    }

    @Query("SELECT * FROM information_systems ORDER BY createdAt")
    suspend fun systems(): List<InformationSystemEntity>

    @Query("SELECT * FROM information_systems WHERE id = :id LIMIT 1")
    suspend fun system(id: String): InformationSystemEntity?

    @Query("SELECT * FROM system_dimensions WHERE systemId = :systemId")
    suspend fun dimensions(systemId: String): List<SystemDimensionEntity>

    @Query("DELETE FROM information_systems WHERE id = :id")
    suspend fun deleteSystem(id: String)
}

@Dao
interface RequirementResponseDao {
    @Upsert
    suspend fun upsert(response: RequirementResponseEntity)

    @Query(
        "SELECT * FROM requirement_responses " +
            "WHERE systemId = :systemId AND requirementId = :requirementId LIMIT 1",
    )
    suspend fun response(systemId: String, requirementId: String): RequirementResponseEntity?

    @Query("SELECT * FROM requirement_responses WHERE systemId = :systemId")
    suspend fun responses(systemId: String): List<RequirementResponseEntity>
}

@Dao
interface DisjunctiveSelectionDao {
    @Upsert
    suspend fun upsert(selection: SystemDisjunctiveSelectionEntity)

    @Query(
        "SELECT * FROM system_disjunctive_selections " +
            "WHERE systemId = :systemId AND parameterId = :parameterId LIMIT 1",
    )
    suspend fun selection(systemId: String, parameterId: String): SystemDisjunctiveSelectionEntity?

    @Query("SELECT * FROM system_disjunctive_selections WHERE systemId = :systemId")
    suspend fun selections(systemId: String): List<SystemDisjunctiveSelectionEntity>
}
