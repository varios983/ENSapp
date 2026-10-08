package com.ensapp.data.catalog

import android.content.res.AssetManager
import com.ensapp.data.local.CatalogDao
import java.security.MessageDigest
import java.nio.charset.StandardCharsets

class CatalogAssetInitializer(
    private val assets: AssetManager,
    private val dao: CatalogDao,
    private val importer: CatalogJsonImporter = CatalogJsonImporter(),
) {
    suspend fun ensureImported() {
        val bytes = assets.open(CATALOG_ASSET).use { it.readBytes() }
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
        if (dao.activeRevision()?.checksum == checksum) return

        val catalog = importer.import(String(bytes, StandardCharsets.UTF_8))
        dao.import(catalog)
    }

    private companion object {
        const val CATALOG_ASSET = "catalog/ENS_Anexo_II_rev_10.json"
    }
}
