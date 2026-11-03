package com.atlas.app.feature.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlas.app.AtlasApp
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.navigation.Destination
import com.atlas.app.data.db.ArticleEntity
import com.atlas.app.data.db.TopicCount
import com.atlas.app.data.feed.SourceKind
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.feature.common.SyncBar
import com.atlas.app.feature.common.openUrl
import com.atlas.app.feature.common.rememberCategories
import com.atlas.app.feature.common.relativeTime
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val windows = listOf(
    "1" to "24 saat",
    "3" to "3 gün",
    "7" to "7 gün",
    "30" to "30 gün",
)

@OptIn(ExperimentalCoroutinesApi::class)
class IntelligenceViewModel(private val app: AtlasApp) : ViewModel() {
    private val db = app.database
    val category = MutableStateFlow("")
    val query = MutableStateFlow("")
    val windowDays = MutableStateFlow("7")
    val onlyKind = MutableStateFlow("")

    private fun since(days: String) = System.currentTimeMillis() - days.toLong() * 24 * 3600_000

    val articles: StateFlow<List<ArticleEntity>> =
        combine(category, query, windowDays) { c, q, w -> Triple(c, q.trim(), w) }
            .flatMapLatest { (c, q, w) -> db.articleDao().observeFiltered(c, q, since(w), 300) }
            .combine(onlyKind) { list, kind -> if (kind.isEmpty()) list else list.filter { it.kind == kind } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val counts: StateFlow<List<TopicCount>> = windowDays
        .flatMapLatest { db.articleDao().observeCategoryCounts(since(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sourceCount: StateFlow<Int> = windowDays
        .flatMapLatest { db.articleDao().observeActiveSourceCount(since(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleSaved(a: ArticleEntity) {
        viewModelScope.launch { db.articleDao().setSaved(a.id, !a.saved, System.currentTimeMillis()) }
    }

    fun saveNote(a: ArticleEntity, note: String, tags: String) {
        viewModelScope.launch {
            db.articleDao().setNote(a.id, note.trim())
            db.articleDao().setTags(a.id, tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","))
        }
    }
}

@Composable
fun IntelligenceScreen() {
    val app = rememberAtlasApp()
    val vm: IntelligenceViewModel = viewModel(factory = viewModelFactory { initializer { IntelligenceViewModel(app) } })
    val articles by vm.articles.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    val sources by vm.sourceCount.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val window by vm.windowDays.collectAsStateWithLifecycle()
    val kind by vm.onlyKind.collectAsStateWithLifecycle()
    val countMap = counts.associate { it.topic to it.count }
    val cats = rememberCategories(app)
    val names = cats.associate { it.id to it.name }
    val total = counts.sumOf { it.count }

    ScreenFrame(Destination.DailyIntelligence.sectionLabel, "Gündem ve haber akışı") {
        SyncBar(app)
        MetaText(
            "Özetler kaynakların kendi metnidir; ATLAS haber yazmaz veya yorumlamaz. " +
                "Seçili dönemde $total başlık, $sources farklı kaynaktan.",
        )
        FilterRow(windows, window, { vm.windowDays.value = it })
        FilterRow(
            options = listOf("" to "Tümü · $total") + cats.map { it.id to "${it.name} · ${countMap[it.id] ?: 0}" },
            selected = category,
            onSelect = { vm.category.value = it },
        )
        FilterRow(
            options = listOf("" to "Tüm türler") + SourceKind.entries.map { it.name to it.label },
            selected = kind,
            onSelect = { vm.onlyKind.value = it },
        )
        AtlasField(query, { vm.query.value = it }, "Başlık, özet, etiket veya kaynakta ara", Modifier.fillMaxWidth(), singleLine = true)

        if (articles.isEmpty()) {
            EmptyHint(
                "Bu filtrelerle kayıt yok. Henüz hiç eşitleme yapılmadıysa 'Şimdi eşitle'ye dokun " +
                    "(internet gerekir). Daha önce indirilenler çevrimdışı da görünür.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(articles, key = { it.id }) { a -> ArticleCard(a, names[a.category] ?: "Diğer", { vm.toggleSaved(a) }, { n, t -> vm.saveNote(a, n, t) }) }
            }
        }
    }
}

@Composable
fun ArticleCard(a: ArticleEntity, categoryName: String, onToggleSaved: () -> Unit, onSaveNote: (String, String) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var note by remember(a.id, a.note) { mutableStateOf(a.note) }
    var tags by remember(a.id, a.tags) { mutableStateOf(a.tags) }
    val kind = SourceKind.fromName(a.kind)

    AtlasPanel(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasChip(kind.label, highlight = kind == SourceKind.OPINION)
            AtlasChip(categoryName)
            if (a.language == "tr") AtlasChip("TR")
        }
        Text(a.title, style = MaterialTheme.typography.titleMedium, color = AtlasColors.Ivory)
        MetaText("${a.sourceName} · ${relativeTime(a.publishedAt)}")
        if (a.summary.isNotBlank()) {
            Text(
                a.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
            )
        }
        if (kind == SourceKind.OPINION) {
            MetaText("Bu kaynak görüş/yorum içerir; olgu ile görüşü ayırmak sana kalır.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasOutlinedButton("Kaynağı aç", { openUrl(context, a.url) })
            AtlasTextButton(if (a.saved) "Kaydedildi ✓" else "Kaydet", onToggleSaved)
            AtlasTextButton(if (expanded) "Daralt" else "Ayrıntı / not", { expanded = !expanded })
        }
        if (expanded) {
            AtlasField(tags, { tags = it }, "Etiketler (virgülle ayır)", Modifier.fillMaxWidth(), singleLine = true)
            AtlasField(note, { note = it }, "Kendi notun (kaynaktan ayrı tutulur)", Modifier.fillMaxWidth(), minLines = 2)
            AtlasOutlinedButton("Notu kaydet", { onSaveNote(note, tags) })
            val vocabApp = com.atlas.app.rememberAtlasApp()
            val vocabScope = androidx.compose.runtime.rememberCoroutineScope()
            var vWord by remember(a.id) { mutableStateOf("") }
            var vMeaning by remember(a.id) { mutableStateOf("") }
            MetaText("Bu haberde bilmediğin bir sözcük mü var? Sözlüğüne ekle (anlamı Sözlük sekmesindeki Tureng'den bakabilirsin).")
            AtlasField(vWord, { vWord = it }, "Sözcük", Modifier.fillMaxWidth(), singleLine = true)
            AtlasField(vMeaning, { vMeaning = it }, "Anlamı", Modifier.fillMaxWidth(), singleLine = true)
            AtlasOutlinedButton("Sözlüğe ekle", {
                vocabScope.launch {
                    vocabApp.database.vocabDao().insert(
                        com.atlas.app.data.db.VocabEntity(word = vWord.trim(), meaning = vMeaning.trim(), context = a.title.take(120)),
                    )
                    vWord = ""
                    vMeaning = ""
                }
            }, enabled = vWord.isNotBlank() && vMeaning.isNotBlank())
            MetaText("Adres: ${a.url}")
        }
    }
}
