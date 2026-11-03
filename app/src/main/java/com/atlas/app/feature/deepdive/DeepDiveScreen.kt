package com.atlas.app.feature.deepdive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.ai.AiFactory
import com.atlas.app.ai.AiMessage
import com.atlas.app.ai.AiNotConfigured
import com.atlas.app.ai.AiPrompts
import com.atlas.app.core.design.AtlasButton
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.core.util.Format
import com.atlas.app.data.db.DeepDiveMessageEntity
import com.atlas.app.data.db.DeepDiveSessionEntity
import com.atlas.app.data.db.PaperEntity
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.openUrl
import com.atlas.app.feature.common.relativeTime
import com.atlas.app.network.ArxivClient
import com.atlas.app.network.PaperDraft
import com.atlas.app.network.PubMedClient
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeepDiveScreen() {
    var sessionId by rememberSaveable { mutableStateOf(-1L) }
    if (sessionId < 0) SessionList(onOpen = { sessionId = it }) else SessionDetail(sessionId, onBack = { sessionId = -1L })
}

@Composable
private fun SessionList(onOpen: (Long) -> Unit) {
    val app = rememberAtlasApp()
    val dao = app.database.deepDiveDao()
    val scope = rememberCoroutineScope()
    val sessions by dao.observeSessions().collectAsStateWithLifecycle(emptyList())
    var title by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<DeepDiveSessionEntity?>(null) }

    ScreenFrame(Destination.DeepDive.sectionLabel, "Derinlemesine araştırma") {
        MetaText(
            "Yapay zekâ OLMADAN da çalışır: çalışma sayfası (varsayımlar, hipotezler, belirsizlikler) ve gerçek arXiv/PubMed aramasıyla okuma listesi. " +
                "Yapay zekâ yardımcısı isteğe bağlıdır; Ayarlar'dan açılır.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasField(title, { title = it }, "Araştırma sorusu / başlık", Modifier.weight(1f), singleLine = true)
            AtlasButton("Başlat", {
                val t = title.trim()
                title = ""
                scope.launch { onOpen(dao.insertSession(DeepDiveSessionEntity(title = t, mode = "OFFLINE"))) }
            }, enabled = title.isNotBlank())
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (sessions.isEmpty()) item { EmptyHint("Henüz oturum yok. Bir soruyla başla.") }
            items(sessions, key = { it.id }) { s ->
                AtlasPanel(Modifier.fillMaxWidth()) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, color = AtlasColors.Ivory)
                    MetaText("Güncellendi: ${Format.timestamp(s.updatedAt)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasOutlinedButton("Aç", { onOpen(s.id) })
                        AtlasTextButton("Sil", { toDelete = s })
                    }
                }
            }
        }
    }
    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Oturum silinsin mi?") },
            text = { Text("'${s.title}' ve sohbet geçmişi kalıcı olarak silinir.") },
            confirmButton = { AtlasTextButton("Sil", { scope.launch { dao.deleteSession(s.id) }; toDelete = null }) },
            dismissButton = { AtlasTextButton("Vazgeç", { toDelete = null }) },
        )
    }
}

@Composable
private fun SessionDetail(id: Long, onBack: () -> Unit) {
    val app = rememberAtlasApp()
    val dao = app.database.deepDiveDao()
    val session by dao.observeSession(id).collectAsStateWithLifecycle(null)
    val s = session
    ScreenFrame(Destination.DeepDive.sectionLabel, s?.title ?: "Oturum") {
        AtlasOutlinedButton("‹ Oturumlar", onBack)
        if (s == null) {
            EmptyHint("Yükleniyor…")
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth >= 900.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { Worksheet(s) }
                            item { ReadingSearch(s) }
                        }
                        Column(Modifier.weight(1f)) { AssistantPanel(s) }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Worksheet(s) }
                        item { ReadingSearch(s) }
                        item { AssistantPanel(s) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Worksheet(s: DeepDiveSessionEntity) {
    val app = rememberAtlasApp()
    val dao = app.database.deepDiveDao()
    val scope = rememberCoroutineScope()
    var assumptions by remember(s.id) { mutableStateOf(s.assumptions) }
    var hypotheses by remember(s.id) { mutableStateOf(s.hypotheses) }
    var uncertainties by remember(s.id) { mutableStateOf(s.uncertainties) }
    var notes by remember(s.id) { mutableStateOf(s.notes) }
    AtlasPanel(Modifier.fillMaxWidth()) {
        SectionLabel("Çalışma sayfası")
        MetaText("Önce neyi bildiğini, neyi varsaydığını ve neyi bilmediğini ayır.")
        AtlasField(assumptions, { assumptions = it }, "Varsayımlarım", Modifier.fillMaxWidth(), minLines = 2)
        AtlasField(hypotheses, { hypotheses = it }, "Hipotezlerim (test edilebilir olsun)", Modifier.fillMaxWidth(), minLines = 2)
        AtlasField(uncertainties, { uncertainties = it }, "Belirsizlikler / bilmediklerim", Modifier.fillMaxWidth(), minLines = 2)
        AtlasField(notes, { notes = it }, "Notlar", Modifier.fillMaxWidth(), minLines = 3)
        AtlasButton("Çalışma sayfasını kaydet", {
            scope.launch {
                dao.updateSession(s.copy(assumptions = assumptions, hypotheses = hypotheses, uncertainties = uncertainties, notes = notes, updatedAt = System.currentTimeMillis()))
            }
        })
    }
}

@Composable
private fun ReadingSearch(s: DeepDiveSessionEntity) {
    val app = rememberAtlasApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable(s.id) { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<PaperDraft>>(emptyList()) }
    var saved by remember { mutableStateOf(setOf<String>()) }

    AtlasPanel(Modifier.fillMaxWidth()) {
        SectionLabel("Okuma listesi · gerçek arama")
        MetaText("arXiv ve PubMed'de İngilizce arama yapar. Sonuçlar kaynaktan olduğu gibi gelir; hiçbiri ATLAS tarafından üretilmez.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasField(query, { query = it }, "Arama ifadesi (İngilizce)", Modifier.weight(1f), singleLine = true)
            AtlasOutlinedButton(if (loading) "Aranıyor…" else "Ara", {
                loading = true
                error = null
                scope.launch {
                    val found = mutableListOf<PaperDraft>()
                    var errs = 0
                    try { found += ArxivClient.search(query, 8) } catch (_: Exception) { errs++ }
                    delay(3300)
                    try { found += PubMedClient.search(query, 8) } catch (_: Exception) { errs++ }
                    results = found
                    error = if (errs == 2) "Arama yapılamadı (internet var mı?)" else if (errs == 1) "Kaynaklardan biri yanıt vermedi." else null
                    loading = false
                }
            }, enabled = query.isNotBlank() && !loading)
        }
        error?.let { MetaText(it) }
        results.forEach { d ->
            val key = d.source + d.externalId
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AtlasChip(if (d.source == "ARXIV") "arXiv · ön baskı" else "PubMed · indeks")
                Text(d.title, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                MetaText("${d.authors.take(120)} · ${d.journal} · ${relativeTime(d.publishedAt)}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasOutlinedButton("Aç", { openUrl(context, d.url) })
                    AtlasTextButton(if (key in saved) "Kütüphanede ✓" else "Kütüphaneye kaydet", {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            app.database.paperDao().insertAll(
                                listOf(
                                    PaperEntity(
                                        source = d.source, externalId = d.externalId, title = d.title, authors = d.authors,
                                        abstractText = d.abstractText, publishedAt = d.publishedAt.coerceAtMost(now), url = d.url,
                                        doi = d.doi, journal = d.journal, topic = "Deep Dive: ${s.title}".take(60), fetchedAt = now,
                                    ),
                                ),
                            )
                            app.database.paperDao().markSaved(d.source, d.externalId, now)
                            saved = saved + key
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun AssistantPanel(s: DeepDiveSessionEntity) {
    val app = rememberAtlasApp()
    val dao = app.database.deepDiveDao()
    val scope = rememberCoroutineScope()
    val messages by dao.observeMessages(s.id).collectAsStateWithLifecycle(emptyList())
    var input by rememberSaveable(s.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var includeSheet by rememberSaveable { mutableStateOf(false) }
    val configured = app.secrets.provider != "NONE" && app.secrets.hasApiKey() && app.secrets.eligibilityAcknowledged

    AtlasPanel(Modifier.fillMaxWidth()) {
        SectionLabel("Yapay zekâ yardımcısı (isteğe bağlı)")
        if (!configured) {
            Text(
                "Kapalı. Uygulamanın geri kalanı yapay zekâ olmadan tam çalışır. Açmak için Ayarlar → Yapay zekâ bölümünü oku; " +
                    "kullanım koşulları (ör. yaş sınırı) senin sorumluluğundadır.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            MetaText("Mesajların seçili sağlayıcıya gönderilir. Yanıtlar doğrulanmamıştır; kaynakları kendin kontrol et.")
            messages.forEach { m -> Bubble(m) }
            error?.let { MetaText(it) }
            AtlasField(input, { input = it }, "Sorun", Modifier.fillMaxWidth(), minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AtlasChip(
                    if (includeSheet) "Çalışma sayfası da gönderilir" else "Çalışma sayfası gönderilmez",
                    highlight = includeSheet,
                    modifier = Modifier.clickable { includeSheet = !includeSheet },
                )
                AtlasButton(if (sending) "Bekleniyor…" else "Gönder", {
                    val text = input.trim()
                    input = ""
                    sending = true
                    error = null
                    scope.launch {
                        try {
                            dao.insertMessage(DeepDiveMessageEntity(sessionId = s.id, role = "USER", content = text))
                            val provider = AiFactory.create(app.secrets)
                            val history = dao.observeMessagesOnce(s.id).map { AiMessage(if (it.role == "USER") "user" else "assistant", it.content) }
                            val system = AiPrompts.DEEP_DIVE + if (includeSheet) {
                                "\n\nKullanıcının çalışma sayfası:\nVarsayımlar: ${s.assumptions}\nHipotezler: ${s.hypotheses}\nBelirsizlikler: ${s.uncertainties}"
                            } else ""
                            val reply = provider.chat(system, history)
                            dao.insertMessage(DeepDiveMessageEntity(sessionId = s.id, role = "AI", content = reply))
                        } catch (e: AiNotConfigured) {
                            error = e.message
                        } catch (e: Exception) {
                            error = "Yapay zekâ isteği başarısız: ${(e.message ?: "bilinmeyen hata").take(200)}"
                        } finally {
                            sending = false
                        }
                    }
                }, enabled = input.isNotBlank() && !sending)
            }
        }
    }
}

@Composable
private fun Bubble(m: DeepDiveMessageEntity) {
    val mine = m.role == "USER"
    AtlasPanel(
        Modifier.fillMaxWidth(),
        container = if (mine) AtlasColors.Surface2 else AtlasColors.Charcoal,
    ) {
        if (!mine) AtlasChip("Yapay zekâ çıktısı — doğrulanmamış", highlight = true)
        else MetaText("Sen")
        Text(m.content, style = MaterialTheme.typography.bodyMedium)
    }
}
