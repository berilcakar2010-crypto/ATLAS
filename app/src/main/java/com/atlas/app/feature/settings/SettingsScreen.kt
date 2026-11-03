package com.atlas.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlas.app.core.design.AtlasButton
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.HairlineDivider
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.data.db.AtlasDatabase
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel(private val db: AtlasDatabase) : ViewModel() {
    /** Tüm yerel tabloları boşaltır. Geri alınamaz; yalnızca açık kullanıcı onayıyla çağrılır. */
    fun deleteAllLocalData() {
        viewModelScope.launch(Dispatchers.IO) { db.clearAllTables() }
    }
}

private val WorkingNow = listOf(
    "Kalıcı sol gezinme (geniş ekranda kenar çubuğu, dar ekranda ray)",
    "Yerel SQLite / Room veritabanı",
    "Soru Motoru: soru ekleme, durum değiştirme, silme",
    "Kütüphane: not ekleme, favori, silme",
    "Command Center: saat, tarih ve veritabanından canlı sayaçlar",
    "Tüm yerel verileri silme",
)

private val NotYet = listOf(
    "Daily Intelligence ve RSS (Phase 4)",
    "Research Radar, arXiv, PubMed (Phase 4)",
    "Sürdürülebilirlik İzleyici (Phase 4)",
    "Çevrimdışı arşiv ve senkronizasyon durumları (Phase 4)",
    "S Pen defteri (Phase 5)",
    "Deep Dive ve yapay zekâ sağlayıcıları (Phase 6)",
    "Bilgi grafiği, içe/dışa aktarma, dashboard modüllerini düzenleme",
)

@Composable
fun SettingsScreen() {
    val app = rememberAtlasApp()
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(app.database) } },
    )
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenFrame(section = Destination.Settings.sectionLabel, title = "Ayarlar") {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AtlasPanel(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                SectionLabel("Veri ve gizlilik")
                HairlineDivider()
                Text(
                    text = "Notların, soruların ve kütüphane kayıtların yalnızca bu cihazda, uygulamanın kendi " +
                        "veritabanında saklanır. Bu sürümde uygulamanın internet izni yoktur ve hiçbir veri " +
                        "cihazdan dışarı gönderilmez. Yapay zekâ henüz bağlı değildir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtlasColors.Ivory,
                )
                AtlasButton(
                    text = "Tüm yerel verileri sil",
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AtlasPanel(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                SectionLabel("Uygulama durumu · v$versionName · Phase 3")
                HairlineDivider()
                SectionLabel("Çalışıyor", color = AtlasColors.TaupeText)
                WorkingNow.forEach { line ->
                    Text(
                        text = "✓ $line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtlasColors.Ivory,
                    )
                }
                HairlineDivider()
                SectionLabel("Henüz yok", color = AtlasColors.TaupeText)
                NotYet.forEach { line ->
                    Text(
                        text = "— $line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtlasColors.TaupeText,
                    )
                }
                MetaText("Bu liste, yazılmış ve çalışan özellikleri gösterir; planlananları değil.")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AtlasColors.Charcoal,
            title = { Text("Tüm yerel veriler silinsin mi?") },
            text = {
                Text(
                    "Sorular ve kütüphane notları bu cihazdan kalıcı olarak silinir. Bu işlem geri alınamaz.",
                )
            },
            confirmButton = {
                AtlasTextButton(
                    text = "Evet, sil",
                    onClick = {
                        vm.deleteAllLocalData()
                        confirmDelete = false
                    },
                )
            },
            dismissButton = {
                AtlasTextButton(text = "Vazgeç", onClick = { confirmDelete = false })
            },
        )
    }
}
