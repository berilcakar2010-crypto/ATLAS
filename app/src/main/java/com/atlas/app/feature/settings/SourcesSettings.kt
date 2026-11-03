package com.atlas.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.HairlineDivider
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.data.db.CategoryEntity
import com.atlas.app.data.db.FeedSourceEntity
import com.atlas.app.data.feed.SourceKind
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.feature.common.rememberCategories
import com.atlas.app.feature.common.relativeTime
import com.atlas.app.network.FeedParser
import com.atlas.app.network.Http
import com.atlas.app.network.TextUtil
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.launch

@Composable
fun CategoryManager(modifier: Modifier) {
    val app = rememberAtlasApp()
    val dao = app.database.categoryDao()
    val scope = rememberCoroutineScope()
    val cats = rememberCategories(app)
    var editing by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newKeywords by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }

    AtlasPanel(modifier) {
        SectionLabel("Kategoriler · ${cats.size}")
        HairlineDivider()
        MetaText(
            "Kategorileri düzenleyebilir, kendi kategorini ekleyebilirsin. Anahtar sözcükler, karma kaynakların " +
                "(Nature, The Conversation vb.) haberlerini otomatik sınıflandırmak içindir: virgülle ayır; " +
                "'nöron*' önek eşleşmesi, 'black hole' ifade eşleşmesi demektir. Sınıflandırma kaba bir sözcük sayımıdır, yanılabilir. " +
                "Yerleşik kategoriler silinemez, yeniden adlandırılabilir.",
        )
        cats.forEach { c ->
            if (editing == c.id) {
                var name by remember(c.id) { mutableStateOf(c.name) }
                var kw by remember(c.id) { mutableStateOf(c.keywords) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasField(name, { name = it }, "Ad", Modifier.fillMaxWidth(), singleLine = true)
                    AtlasField(kw, { kw = it }, "Anahtar sözcükler (İngilizce, virgülle)", Modifier.fillMaxWidth(), minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasOutlinedButton("Kaydet", {
                            scope.launch { dao.upsert(c.copy(name = name.trim().ifEmpty { c.name }, keywords = kw.trim())) }
                            editing = null
                        })
                        AtlasTextButton("Vazgeç", { editing = null })
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory)
                        MetaText(c.keywords.take(90).ifBlank { "anahtar sözcük yok" } + if (c.keywords.length > 90) "…" else "")
                    }
                    if (!c.builtin) AtlasChip("Benim")
                    AtlasTextButton("Düzenle", { editing = c.id })
                    if (!c.builtin) AtlasTextButton("Sil", {
                        scope.launch { dao.deleteCustom(c.id) }
                    })
                }
            }
        }
        HairlineDivider()
        SectionLabel("Yeni kategori", color = AtlasColors.TaupeText)
        AtlasField(newName, { newName = it }, "Kategori adı", Modifier.fillMaxWidth(), singleLine = true)
        AtlasField(newKeywords, { newKeywords = it }, "Anahtar sözcükler (İngilizce, virgülle; isteğe bağlı)", Modifier.fillMaxWidth(), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasOutlinedButton("Kategori ekle", {
                scope.launch {
                    val order = (dao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
                    dao.upsert(
                        CategoryEntity(
                            id = "custom-" + System.currentTimeMillis(), name = newName.trim(),
                            keywords = newKeywords.trim(), builtin = false, sortOrder = order,
                        ),
                    )
                    newName = ""
                    newKeywords = ""
                }
            }, enabled = newName.isNotBlank())
            AtlasOutlinedButton("Haberleri yeniden sınıflandır", {
                scope.launch {
                    val n = app.syncManager.reclassify()
                    info = "$n haberin kategorisi değişti."
                }
            })
        }
        info?.let { MetaText(it) }
    }
}

@Composable
private fun CategoryPicker(current: String, cats: List<CategoryEntity>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AtlasTextButton(cats.firstOrNull { it.id == current }?.name ?: "Kategori ▾", { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            cats.forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = { onPick(c.id); open = false })
            }
        }
    }
}

@Composable
fun FeedManager(modifier: Modifier) {
    val app = rememberAtlasApp()
    val dao = app.database.feedDao()
    val scope = rememberCoroutineScope()
    val cats = rememberCategories(app)
    val feeds by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    var filter by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var catId by remember { mutableStateOf("SCIENCE") }
    var kind by remember { mutableStateOf(SourceKind.JOURNALISM.name) }
    var lang by remember { mutableStateOf("en") }
    var info by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AtlasPanel(modifier) {
        SectionLabel("Haber kaynakları · ${feeds.count { it.enabled }}/${feeds.size} açık")
        HairlineDivider()
        MetaText("Kaynağı kapatabilir, kategorisini değiştirebilir veya kendi RSS/Atom adresini ekleyebilirsin. Kapatılan kaynağın eski kayıtları silinmez.")
        AtlasField(filter, { filter = it }, "Kaynak ara", Modifier.fillMaxWidth(), singleLine = true)
        feeds.filter { filter.isBlank() || it.name.contains(filter, true) }.forEach { f -> FeedRow(f, cats, dao, scope) }

        HairlineDivider()
        SectionLabel("Kendi kaynağını ekle", color = AtlasColors.TaupeText)
        AtlasField(name, { name = it }, "Kaynak adı", Modifier.fillMaxWidth(), singleLine = true)
        AtlasField(url, { url = it }, "RSS / Atom adresi (https://…)", Modifier.fillMaxWidth(), singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetaText("Kategori:")
            CategoryPicker(catId, cats, { catId = it })
        }
        FilterRow(SourceKind.entries.map { it.name to it.label }, kind, { kind = it })
        FilterRow(listOf("en" to "İngilizce", "tr" to "Türkçe"), lang, { lang = it })
        AtlasOutlinedButton(if (busy) "Deneniyor…" else "Dene ve ekle", {
            val u = url.trim()
            busy = true
            info = null
            scope.launch {
                try {
                    if (!TextUtil.isHttpUrl(u)) error("Adres http:// veya https:// ile başlamalı")
                    val items = FeedParser.parse(Http.get(u), 5)
                    if (items.isEmpty()) error("Adres okundu ama içinde haber bulunamadı")
                    dao.insertAll(
                        listOf(
                            FeedSourceEntity(
                                id = "custom-" + TextUtil.urlKey(u).take(12), name = name.trim().ifEmpty { u },
                                url = u, category = catId, kind = kind, language = lang, mixed = false, enabled = true,
                                lastFetchedAt = 0, lastStatus = "PENDING", lastError = null, lastCount = 0,
                            ),
                        ),
                    )
                    info = "Eklendi (${items.size}+ haber okundu). Sonraki eşitlemede çekilir."
                    name = ""; url = ""
                } catch (e: Exception) {
                    info = "Eklenemedi: ${(e.message ?: "bilinmeyen hata").take(140)}"
                } finally {
                    busy = false
                }
            }
        }, enabled = url.isNotBlank() && !busy)
        info?.let { MetaText(it) }
    }
}

@Composable
private fun FeedRow(
    f: FeedSourceEntity,
    cats: List<CategoryEntity>,
    dao: com.atlas.app.data.db.FeedDao,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        CategoryPicker(f.category, cats) { c -> scope.launch { dao.setCategory(f.id, c) } }
        AtlasTextButton(if (f.enabled) "Açık" else "Kapalı", { scope.launch { dao.setEnabled(f.id, !f.enabled) } })
        if (f.id.startsWith("custom-")) AtlasTextButton("Sil", { scope.launch { dao.deleteCustom(f.id) } })
    }
}
