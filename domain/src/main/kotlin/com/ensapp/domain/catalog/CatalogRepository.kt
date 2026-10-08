package com.ensapp.domain.catalog

interface CatalogRepository {
    suspend fun activeCatalog(): Catalog
}
