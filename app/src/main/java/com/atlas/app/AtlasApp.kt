package com.atlas.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.atlas.app.data.db.AtlasDatabase

class AtlasApp : Application() {
    /** Basit elle bağımlılık enjeksiyonu; Phase 3 için Hilt gerekmiyor. */
    val database: AtlasDatabase by lazy { AtlasDatabase.create(this) }
}

@Composable
fun rememberAtlasApp(): AtlasApp = LocalContext.current.applicationContext as AtlasApp
