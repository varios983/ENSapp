package com.ensapp.domain.system

import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.SecurityDimension
import java.time.Instant

data class InformationSystem(
    val id: String,
    val name: String,
    val category: EnsCategory,
    val dimensions: Map<SecurityDimension, DimensionLevel>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "El identificador del sistema no puede estar vacío" }
        require(name.isNotBlank()) { "El nombre del sistema no puede estar vacío" }
        require(dimensions.keys == SecurityDimension.entries.toSet()) {
            "El sistema debe tener configuradas exactamente las cinco dimensiones CIDAT"
        }
    }
}
