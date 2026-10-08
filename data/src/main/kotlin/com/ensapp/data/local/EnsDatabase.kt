package com.ensapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CatalogRevisionEntity::class,
        FrameworkEntity::class,
        FamilyEntity::class,
        CatalogControlEntity::class,
        CatalogPropertyEntity::class,
        ApplicabilityRuleEntity::class,
        ApplicabilityValueEntity::class,
        CatalogParameterEntity::class,
        DisjunctiveChoiceEntity::class,
        CatalogPartEntity::class,
        RequirementEntity::class,
        InformationSystemEntity::class,
        SystemDimensionEntity::class,
        RequirementResponseEntity::class,
        SystemDisjunctiveSelectionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EnsDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun informationSystemDao(): InformationSystemDao
    abstract fun responseDao(): RequirementResponseDao
    abstract fun disjunctiveSelectionDao(): DisjunctiveSelectionDao
}
