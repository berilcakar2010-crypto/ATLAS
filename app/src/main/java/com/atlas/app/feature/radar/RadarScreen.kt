package com.atlas.app.feature.radar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.data.db.PaperEntity
import com.atlas.app.data.db.ResearchTopicEntity
import com.atlas.app.data.db.TopicCount
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.feature.common.SyncBar
import com.atlas.app.feature.common.openUrl
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

private val windows = listOf("30" to "30 gün", "90" to "90 gün", "365" to "1 yıl", "36500" to "Hepsi")

@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModel(private val app: AtlasApp) : ViewModel() {
    private val db = app.database
    val topic = MutableStateFlow("")
    val query = MutableStateFlow("")
    val windowDays = MutableStateFlow("365")
    val source = MutableStateFlow("")
    val refreshing = MutableStateFlow(false)

    val papers: StateFlow<List<PaperEntity>> =
        combine(topic, query, windowDays) { t, q, w -> Triple(t, q.trim(), w) }
            .flatMapLatest { (t, q, w) ->
                db.paperDao().observeFiltered(t, q, System.currentTimeMillis() - w.toLong() * 24 * 3600_000, 300)
            }
            .combine(source) { list, s -> if (s.isEmpty()) list else list.filter { it.source == s } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topics: StateFlow<List<ResearchTopicEntity>> = db.topicDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topicCounts: StateFlow<List<TopicCount>> = db.paperDao().observeTopicCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSaved(p: PaperEntity) {
        viewModelScope.launch { db.paperDao().setSaved(p.id, !p.saved, System.currentTimeMillis()) }
    }

    fun saveAnnotation(p: PaperEntity, annotation: String, tags: String) {
        viewModelScope.launch {
            db.paperDao().setAnnotation(p.id, annotation.trim())
            db.paperDao().setTags(p.id, tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","))
        }
    }

    fun addTopic(name: String, query: String) {
        viewModelScope.launch { db.topicDao().insert(ResearchTopicEntity(name = name.trim(), query = query.trim())) }
    }

    fun setTopicEnabled(t: ResearchTopicEntity, enabled: Boolean) {
        viewModelScope.launch { db.topicDao().setEnabled(t.id, enabled) }
    }

    fun deleteTopic(t: ResearchTopicEntity) {
        viewModelScope.launch { db.topicDao().delete(t.id) }
    }

    fun refreshTopic(t: ResearchTopicEntity) {
        if (refreshing.value) return
        refreshing.value = true
        app.appScope.launch {
            try {
                app.syncManager.refreshTopic(t.name, t.query)
            } finally {
                refreshing.value = false
            }
        }
    }
}

@Composable
fun RadarScreen() {
    val app = rememberAtlasApp()
    val vm: RadarViewModel = viewModel(factory = viewModelFactory { initializer { RadarViewModel(app) } })
    val papers by vm.papers.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val counts by vm.topicCounts.collectAsStateWithLifecycle()
    val topic by vm.topic.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val window by vm.windowDays.collectAsStateWithLifecycle()
    val source by vm.source.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var managing by remember { mutableStateOf(false) }
    val countMap = counts.associate { it.topic to it.count }

    ScreenFrame(Destination.ResearchRadar.sectionLabel, "Makale radarı") {
        SyncBar(app)
        MetaText(
            "Kaynaklar: arXiv (ön baskı, hakem denetimi yok) ve PubMed (indeks; her kayıt hakemli değildir). " +
                "Başlık, yazar, özet ve tarih kaynaktan gelir; ATLAS bunları yazmaz.",
        )
        FilterRow(
            options = listOf("" to "Tüm konular") + topics.map { it.name to "${it.name} · ${countMap[it.name] ?: 0}" },
            selected = topic,
            onSelect = { vm.topic.value = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterRow(windows, window, { vm.windowDays.value = it })
            FilterRow(
                listOf("" to "Tüm kaynaklar", "ARXIV" to "arXiv", "PUBMED" to "PubMed"),
                source,
                { vm.source.value = it },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasField(query, { vm.query.value = it }, "Başlık, yazar, özet veya etiket", Modifier.weight(1f), singleLine = true)
            AtlasOutlinedButton(if (managing) "Konuları gizle" else "Konuları yönet", { managing = !managing })
        }
        if (managing) TopicManager(vm, topics, refreshing)

        if (papers.isEmpty()) {
            EmptyHint(
                "Bu filtrelerle makale yok. 'Şimdi eşitle' ilk makaleleri indirir (16 konu için bir iki dakika sürebilir; " +
                    "arXiv istekler arasında bekleme ister). İndirilenler çevrimdışı da okunur.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(papers, key = { it.id }) { p -> PaperCard(p, vm) }
            }
        }
    }
}

@Composable
private fun TopicManager(vm: RadarViewModel, topics: List<ResearchTopicEntity>, refreshing: Boolean) {
    var name by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    AtlasPanel(Modifier.fillMaxWidth()) {
        SectionLabel("Takip edilen konular")
        MetaText("Arama ifadesi İngilizce olmalı (arXiv/PubMed İngilizce indeksler). Tek ifade, en iyi sonuç için kısa tut.")
        topics.forEach { t ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${t.name}  —  ${t.query}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (t.enabled) AtlasColors.Ivory else AtlasColors.TaupeText,
                )
                AtlasTextButton(if (t.enabled) "Açık" else "Kapalı", { vm.setTopicEnabled(t, !t.enabled) })
                AtlasTextButton(if (refreshing) "…" else "Yenile", { vm.refreshTopic(t) })
                AtlasTextButton("Sil", { vm.deleteTopic(t) })
            }
        }
        AtlasField(name, { name = it }, "Konu adı", Modifier.fillMaxWidth(), singleLine = true)
        AtlasField(q, { q = it }, "Arama ifadesi (İngilizce)", Modifier.fillMaxWidth(), singleLine = true)
        AtlasOutlinedButton("Konu ekle", {
            vm.addTopic(name, q)
            name = ""
            q = ""
        }, enabled = name.isNotBlank() && q.isNotBlank())
    }
}

@Composable
private fun PaperCard(p: PaperEntity, vm: RadarViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var note by remember(p.id, p.annotation) { mutableStateOf(p.annotation) }
    var tags by remember(p.id, p.tags) { mutableStateOf(p.tags) }

    AtlasPanel(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasChip(if (p.source == "ARXIV") "arXiv · ön baskı" else "PubMed · indeks", highlight = p.source == "PUBMED")
            AtlasChip(p.topic)
        }
        Text(p.title, style = MaterialTheme.typography.titleMedium, color = AtlasColors.Ivory)
        if (p.authors.isNotBlank()) {
            MetaText(if (expanded || p.authors.length < 140) p.authors else p.authors.take(140) + "…")
        }
        MetaText("${p.journal} · ${relativeTime(p.publishedAt)}" + (p.doi?.let { " · DOI: $it" } ?: ""))
        if (p.abstractText.isNotBlank()) {
            Text(
                p.abstractText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
            )
        } else {
            MetaText("Bu kayıt için kaynak özet vermedi.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasOutlinedButton("Makaleyi aç", { openUrl(context, p.url) })
            AtlasTextButton(if (p.saved) "Kaydedildi ✓" else "Kaydet", { vm.toggleSaved(p) })
            AtlasTextButton(if (expanded) "Daralt" else "Özet / not", { expanded = !expanded })
        }
        if (expanded) {
            AtlasField(tags, { tags = it }, "Etiketler (virgülle ayır)", Modifier.fillMaxWidth(), singleLine = true)
            AtlasField(note, { note = it }, "Kendi notun / çıkarımın", Modifier.fillMaxWidth(), minLines = 3)
            AtlasOutlinedButton("Notu kaydet", { vm.saveAnnotation(p, note, tags) })
        }
    }
}
