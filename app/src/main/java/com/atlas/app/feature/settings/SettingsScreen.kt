package com.atlas.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.ai.GeminiProvider
import com.atlas.app.core.design.AtlasButton
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.HairlineDivider
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.feature.common.relativeTime
import com.atlas.app.rememberAtlasApp
import com.atlas.app.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val WorkingNow = listOf(
    "Kalıcı sol gezinme (geniş ekranda kenar çubuğu, dar ekranda ray)",
    "Yerel SQLite / Room veritabanı",
    "Soru Motoru ve Kütüphane notları",
    "Daily Intelligence: ~53 kaynaktan RSS/Atom, konu ve tür etiketi, arama, kaydetme, not",
    "Research Radar: arXiv ve PubMed araması, konu takibi, kaydetme, not",
    "Sürdürülebilirlik izleyici (haberler + kendi gözlemlerin) ve Çevrimdışı Arşiv durumu",
    "Bilgi grafiği (etiket/başlık eşleşmesine dayalı)",
    "S Pen defteri: çizim, basınç, silgi, şekiller, kâğıt türleri, geri al, PDF'e aktarma",
    "Deep Dive: çalışma sayfası + gerçek makale araması; isteğe bağlı yapay zekâ",
    "Command Center: canlı özet, düzenlenebilir modüller",
)

private val NotYet = listOf(
    "El yazısını metne çevirme (yok; arama yalnızca başlıklarda)",
    "Defterde metin kutusu, görsel ekleme, PDF üstüne not alma",
    "Haber tam metni / PDF indirme (yalnızca başlık, özet, bağlantı saklanır)",
    "İçe/dışa aktarma ve yedekleme (PDF dışında)",
    "Gerçek cihazda (Galaxy Tab S10 Lite + S Pen) doğrulama: henüz yapılmadı, ilk testi sen yapacaksın",
)

@Composable
fun SettingsScreen() {
    val app = rememberAtlasApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var bgSync by remember { mutableStateOf(app.secrets.backgroundSync) }
    val feeds by app.database.feedDao().observeAll().collectAsStateWithLifecycle(emptyList())

    ScreenFrame(section = Destination.Settings.sectionLabel, title = "Ayarlar") {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AtlasPanel(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                SectionLabel("Veri ve gizlilik")
                HairlineDivider()
                Text(
                    text = "Notların, sorların, defterin ve kayıtların yalnızca bu cihazda saklanır. Uygulama internete şunlar için " +
                        "bağlanır: haber kaynaklarının herkese açık RSS adresleri, arXiv ve PubMed arama servisleri. " +
                        "Bu isteklerde kişisel verin, notların veya aramaların dışında hiçbir bilgi gönderilmez " +
                        "(Radar'daki konu arama ifadeleri arXiv/PubMed'e gider). Yapay zekâ varsayılan olarak KAPALIDIR.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtlasColors.Ivory,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AtlasChip(
                        if (bgSync) "Arka plan eşitleme: açık (6 saatte bir)" else "Arka plan eşitleme: kapalı",
                        highlight = bgSync,
                        modifier = Modifier.clickable {
                            bgSync = !bgSync
                            app.secrets.backgroundSync = bgSync
                            if (bgSync) SyncWorker.schedule(context) else SyncWorker.cancel(context)
                        },
                    )
                }
                AtlasButton("Tüm yerel verileri sil", { confirmDelete = true }, Modifier.fillMaxWidth())
            }

            AiSettings(Modifier.widthIn(max = 820.dp).fillMaxWidth())

            AtlasPanel(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                SectionLabel("Haber kaynakları · ${feeds.count { it.enabled }}/${feeds.size} açık")
                HairlineDivider()
                MetaText("Kaynakları kapatırsan sonraki eşitlemede çekilmez; eski kayıtlar silinmez.")
                feeds.forEach { f ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium, color = if (f.enabled) AtlasColors.Ivory else AtlasColors.TaupeText)
                            MetaText(
                                when (f.lastStatus) {
                                    "OK" -> "OK · ${relativeTime(f.lastFetchedAt)}"
                                    "FAILED" -> "Başarısız · ${f.lastError ?: ""}"
                                    else -> "Henüz denenmedi"
                                },
                            )
                        }
                        AtlasTextButton(if (f.enabled) "Açık" else "Kapalı", {
                            scope.launch { app.database.feedDao().setEnabled(f.id, !f.enabled) }
                        })
                    }
                }
            }

            AtlasPanel(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                SectionLabel("Uygulama durumu · v$versionName")
                HairlineDivider()
                SectionLabel("Yazıldı", color = AtlasColors.TaupeText)
                WorkingNow.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory) }
                HairlineDivider()
                SectionLabel("Yok / doğrulanmadı", color = AtlasColors.TaupeText)
                NotYet.forEach { Text("— $it", style = MaterialTheme.typography.bodyMedium, color = AtlasColors.TaupeText) }
                MetaText("Bu liste yazılmış özellikleri gösterir; bir özelliğin gerçek cihazda kusursuz çalıştığı iddia edilmez.")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AtlasColors.Charcoal,
            title = { Text("Tüm yerel veriler silinsin mi?") },
            text = { Text("Sorular, notlar, defterler, kayıtlı haber/makaleler, Deep Dive oturumları ve yapay zekâ anahtarı bu cihazdan kalıcı olarak silinir. Geri alınamaz.") },
            confirmButton = {
                AtlasTextButton("Evet, sil", {
                    confirmDelete = false
                    scope.launch {
                        withContext(Dispatchers.IO) { app.database.clearAllTables() }
                        app.secrets.wipe()
                        app.syncManager.seed()
                    }
                })
            },
            dismissButton = { AtlasTextButton("Vazgeç", { confirmDelete = false }) },
        )
    }
}

@Composable
private fun AiSettings(modifier: Modifier) {
    val app = rememberAtlasApp()
    val store = app.secrets
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(store.provider) }
    var ack by remember { mutableStateOf(store.eligibilityAcknowledged) }
    var keyInput by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(store.hasApiKey()) }
    var model by remember { mutableStateOf(store.model) }
    var baseUrl by remember { mutableStateOf(store.baseUrl) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var info by remember { mutableStateOf<String?>(null) }

    AtlasPanel(modifier) {
        SectionLabel("Yapay zekâ (isteğe bağlı)")
        HairlineDivider()
        Text(
            "ATLAS yapay zekâ olmadan tam çalışır. Açarsan şunları bil:",
            style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory,
        )
        listOf(
            "Deep Dive'da yazdığın mesajlar (ve istersen çalışma sayfan) seçtiğin sağlayıcının sunucusuna gönderilir. Başka hiçbir veri gönderilmez.",
            "Kullanım koşulları sağlayıcıya aittir. Örneğin Google'ın Gemini API şartları API'yi kullanmak için 18 yaş ve üzeri olmayı ister; " +
                "kendi yaşın, ailen veya okulunun kuralları açısından uygun değilse AÇMA. ATLAS yaş sınırlarını aşmanı sağlamaz.",
            "API anahtarı bu cihazda Android Keystore ile şifreli saklanır; yine de cihaza erişen biri kullanabilir. Yalnızca kota sınırlı/ücretsiz anahtar kullan, başkasına gösterme.",
            "Yapay zekâ yanılır ve kaynak uydurabilir. Çıktılar 'doğrulanmamış' etiketiyle gösterilir.",
        ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = AtlasColors.TaupeText) }

        AtlasChip(
            if (ack) "Okudum, bu kullanım bana uygun ✓" else "Okudum, bu kullanım bana uygun (onayla)",
            highlight = ack,
            modifier = Modifier.clickable { ack = !ack; store.eligibilityAcknowledged = ack },
        )
        FilterRow(
            listOf("NONE" to "Kapalı", "GEMINI" to "Google Gemini", "OPENAI_COMPAT" to "OpenAI uyumlu uç nokta"),
            provider,
            { provider = it; store.provider = it },
        )
        if (provider != "NONE") {
            if (!ack) {
                MetaText("Devam etmek için yukarıdaki onayı ver.")
            } else {
                if (provider == "OPENAI_COMPAT") {
                    AtlasField(baseUrl, { baseUrl = it; store.baseUrl = it }, "Uç nokta adresi (ör. https://…/v1)", Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text(if (hasKey) "API anahtarı kayıtlı — değiştirmek için yaz" else "API anahtarı") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasOutlinedButton("Anahtarı kaydet", {
                        store.saveApiKey(keyInput)
                        keyInput = ""
                        hasKey = store.hasApiKey()
                        info = if (hasKey) "Anahtar şifrelenip kaydedildi." else "Anahtar silindi."
                    }, enabled = keyInput.isNotBlank())
                    if (hasKey) AtlasTextButton("Anahtarı sil", { store.saveApiKey(""); hasKey = false; info = "Anahtar silindi." })
                }
                if (provider == "GEMINI" && hasKey) {
                    AtlasOutlinedButton("Kullanılabilir modelleri listele", {
                        scope.launch {
                            info = try {
                                models = GeminiProvider.listModels(store.apiKey().orEmpty())
                                if (models.isEmpty()) "Model bulunamadı." else "${models.size} model bulundu; birini seç."
                            } catch (e: Exception) {
                                "Liste alınamadı: ${(e.message ?: "").take(120)}"
                            }
                        }
                    })
                    if (models.isNotEmpty()) {
                        FilterRow(models.take(30).map { it to it }, model, { model = it; store.model = it })
                    }
                }
                AtlasField(model, { model = it; store.model = it }, "Model adı", Modifier.fillMaxWidth(), singleLine = true)
                info?.let { MetaText(it) }
            }
        }
    }
}
