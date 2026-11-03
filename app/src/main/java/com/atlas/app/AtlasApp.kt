package com.atlas.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.atlas.app.ai.SecretStore
import com.atlas.app.data.db.AtlasDatabase
import com.atlas.app.sync.SyncManager
import com.atlas.app.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AtlasApp : Application() {
    /** Basit elle bağımlılık enjeksiyonu; Hilt gerekmiyor. */
    val database: AtlasDatabase by lazy { AtlasDatabase.create(this) }
    val syncManager: SyncManager by lazy { SyncManager(database) }
    val secrets: SecretStore by lazy { SecretStore(this) }

    /** Uygulama ömrü boyunca yaşayan kapsam (elle eşitleme ekran değişse de sürer). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { syncManager.seed() }
        if (secrets.backgroundSync) SyncWorker.schedule(this)
    }

    fun syncNow(forcePapers: Boolean = true) {
        appScope.launch { syncManager.syncAll(includePapers = true, forcePapers = forcePapers) }
    }
}

@Composable
fun rememberAtlasApp(): AtlasApp = LocalContext.current.applicationContext as AtlasApp
