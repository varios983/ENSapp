package com.ensapp

import android.app.Application
import com.ensapp.data.catalog.CatalogAssetInitializer
import com.ensapp.data.local.EnsDatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class EnsApplication : Application() {
    lateinit var database: com.ensapp.data.local.EnsDatabase
        private set

    private val catalogReady = CompletableDeferred<Unit>()

    suspend fun awaitCatalogReady() {
        catalogReady.await()
    }

    override fun onCreate() {
        super.onCreate()
        database = EnsDatabaseProvider.create(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
            .launch {
                CatalogAssetInitializer(assets, database.catalogDao()).ensureImported()
            }
            .invokeOnCompletion { error ->
                if (error == null) catalogReady.complete(Unit)
                else catalogReady.completeExceptionally(error)
            }
    }
}
