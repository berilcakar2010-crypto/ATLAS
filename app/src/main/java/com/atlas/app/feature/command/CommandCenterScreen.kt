package com.atlas.app.feature.command

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.atlas.app.core.design.HairlineDivider
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.MonoFamily
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.core.util.Format
import com.atlas.app.data.db.ArticleEntity
import com.atlas.app.data.db.LibraryItemEntity
import com.atlas.app.data.db.QuestionEntity
import com.atlas.app.data.db.QuestionStatus
import com.atlas.app.data.db.ResearchTopicEntity
import com.atlas.app.data.db.TopicCount
import com.atlas.app.data.feed.Category
import com.atlas.app.feature.common.SyncBar
import com.atlas.app.feature.common.openUrl
import com.atlas.app.feature.common.relativeTime
import com.atlas.app.rememberAtlasApp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CommandCenterViewModel(app: AtlasApp) : ViewModel() {
    private val db = app.database
    private val since = System.currentTimeMillis() - 48L * 3600_000

    val recentLibrary: StateFlow<List<LibraryItemEntity>> = db.libraryDao().observeRecent(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val openQuestions: StateFlow<List<QuestionEntity>> = db.questionDao().observeOpen(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Son 48 saatten her alanın en yeni haberi (en çok 10 alan). Hiçbir şey üretilmez; yalnızca seçilir. */
    val briefing: StateFlow<List<ArticleEntity>> = db.articleDao().observeRecent(since, 300)
        .map { list ->
            list.groupBy { it.category }.values.map { it.first() }.sortedByDescending { it.publishedAt }.take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val briefingTotal: StateFlow<Int> = db.articleDao().observeCategoryCounts(since).map { l -> l.sumOf { it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val briefingSources: StateFlow<Int> = db.articleDao().observeActiveSourceCount(since)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val topics: StateFlow<List<ResearchTopicEntity>> = db.topicDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topicCounts: StateFlow<List<TopicCount>> = db.paperDao().observeTopicCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notebookCount: StateFlow<Int> = db.notebookDao().observePageCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

private val DateFormatter = DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy", Format.turkish)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Format.turkish)

@Composable
fun CommandCenterScreen(onNavigate: (Destination) -> Unit) {
    val app = rememberAtlasApp()
    val context = LocalContext.current
    val vm: CommandCenterViewModel = viewModel(factory = viewModelFactory { initializer { CommandCenterViewModel(app) } })
    val store = remember { LayoutStore(context) }
    var slots by remember { mutableStateOf(store.load()) }
    var editing by remember { mutableStateOf(false) }

    fun update(new: List<Slot>) {
        slots = new
        store.save(new)
    }

    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(15_000)
        }
    }

    ScreenFrame(section = Destination.CommandCenter.sectionLabel, title = Format.greeting(now.hour)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = now.format(TimeFormatter),
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFamily),
                color = AtlasColors.Ivory,
            )
            MetaText(now.format(DateFormatter).uppercase(Format.turkish), Modifier.weight(1f))
            AtlasOutlinedButton(if (editing) "Düzenlemeyi bitir" else "Düzenle", { editing = !editing })
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 860.dp
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val visible = slots.filter { !it.hidden }
                // Geniş ekranda 3 birimlik satırlara diz; dar ekranda alt alta.
                val rows: List<List<Slot>> = if (wide) {
                    buildList {
                        var cur = mutableListOf<Slot>()
                        var sum = 0
                        for (s in visible) {
                            if (sum + s.span > 3 && cur.isNotEmpty()) { add(cur); cur = mutableListOf(); sum = 0 }
                            cur.add(s); sum += s.span
                        }
                        if (cur.isNotEmpty()) add(cur)
                    }
                } else visible.map { listOf(it) }

                rows.forEach { row ->
                    if (wide) {
                        val sum = row.sumOf { it.span }
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { slot ->
                                ModuleCard(slot, editing, slots, ::update, vm, onNavigate, Modifier.weight(slot.span.toFloat()).fillMaxHeight())
                            }
                            if (sum < 3) Spacer(Modifier.weight((3 - sum).toFloat()))
                        }
                    } else {
                        ModuleCard(row[0], editing, slots, ::update, vm, onNavigate, Modifier.fillMaxWidth())
                    }
                }

                if (editing) {
                    val hidden = slots.filter { it.hidden }
                    AtlasPanel(Modifier.fillMaxWidth()) {
                        SectionLabel("Gizli modüller")
                        if (hidden.isEmpty()) MetaText("Gizli modül yok.")
                        hidden.forEach { h ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(h.id.label, style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory)
                                AtlasTextButton("Göster", { update(slots.map { if (it.id == h.id) it.copy(hidden = false) else it }) })
                            }
                        }
                        AtlasTextButton("Varsayılan düzene dön", { store.reset(); slots = store.load() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    slot: Slot,
    editing: Boolean,
    all: List<Slot>,
    update: (List<Slot>) -> Unit,
    vm: CommandCenterViewModel,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (editing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MetaText(slot.id.label, Modifier.weight(1f))
                val idx = all.indexOfFirst { it.id == slot.id }
                AtlasTextButton("↑", { if (idx > 0) update(all.toMutableList().also { val t = it[idx]; it[idx] = it[idx - 1]; it[idx - 1] = t }) })
                AtlasTextButton("↓", { if (idx < all.lastIndex) update(all.toMutableList().also { val t = it[idx]; it[idx] = it[idx + 1]; it[idx + 1] = t }) })
                AtlasTextButton("Boy ${slot.span}", { update(all.map { if (it.id == slot.id) it.copy(span = slot.span % 3 + 1) else it }) })
                AtlasTextButton("Gizle", { update(all.map { if (it.id == slot.id) it.copy(hidden = true) else it }) })
            }
        }
        val inner = Modifier.fillMaxWidth().weight(1f, fill = false)
        when (slot.id) {
            ModuleId.BRIEFING -> BriefingPanel(vm, onNavigate, inner)
            ModuleId.TOPICS -> TopicsPanel(vm, onNavigate, inner)
            ModuleId.SYNC -> SyncPanel(inner)
            ModuleId.RECENT -> RecentPanel(vm, onNavigate, inner)
            ModuleId.QUESTIONS -> QuestionsPanel(vm, onNavigate, inner)
            ModuleId.NOTEBOOK -> NotebookPanel(vm, onNavigate, inner)
        }
    }
}

@Composable
private fun BriefingPanel(vm: CommandCenterViewModel, onNavigate: (Destination) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val items by vm.briefing.collectAsStateWithLifecycle()
    val total by vm.briefingTotal.collectAsStateWithLifecycle()
    val sources by vm.briefingSources.collectAsStateWithLifecycle()
    AtlasPanel(modifier) {
        SectionLabel("Günlük bülten · son 48 saat")
        if (items.isEmpty()) {
            Text(
                "Henüz yerel haber yok. Aşağıdaki 'Şimdi eşitle' ile kaynaklardan indir (internet gerekir). " +
                    "Uydurma özet gösterilmez; burada yalnızca kaynakların gerçek başlıkları çıkar.",
                style = MaterialTheme.typography.bodyLarge, color = AtlasColors.Ivory,
            )
        } else {
            MetaText("$total başlık · $sources kaynak · her alandan en yeni haber")
            items.forEach { a ->
                Column(Modifier.fillMaxWidth().clickable { openUrl(context, a.url) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AtlasChip(Category.fromName(a.category).label, highlight = true)
                        MetaText("${a.sourceName} · ${relativeTime(a.publishedAt)}")
                    }
                    Text(a.title, style = MaterialTheme.typography.bodyLarge, color = AtlasColors.Ivory, maxLines = 2)
                }
            }
        }
        HairlineDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtlasTextButton("Tüm gündem", { onNavigate(Destination.DailyIntelligence) })
            AtlasTextButton("Sürdürülebilirlik", { onNavigate(Destination.Sustainability) })
        }
    }
}

@Composable
private fun TopicsPanel(vm: CommandCenterViewModel, onNavigate: (Destination) -> Unit, modifier: Modifier) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    val counts by vm.topicCounts.collectAsStateWithLifecycle()
    val map = counts.associate { it.topic to it.count }
    AtlasPanel(modifier) {
        SectionLabel("Aktif araştırma konuları")
        topics.filter { it.enabled }.forEach { t ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(t.name, style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory, modifier = Modifier.weight(1f))
                MetaText("${map[t.name] ?: 0} makale")
            }
        }
        MetaText("Sayaç: bu cihazda kayıtlı makale sayısı (kaynakların toplamı değil).")
        AtlasTextButton("Research Radar", { onNavigate(Destination.ResearchRadar) })
    }
}

@Composable
private fun SyncPanel(modifier: Modifier) {
    val app = rememberAtlasApp()
    val feeds by app.database.feedDao().observeAll().collectAsStateWithLifecycle(emptyList())
    AtlasPanel(modifier) {
        SectionLabel("Eşitleme")
        SyncBar(app)
        val ok = feeds.count { it.lastStatus == "OK" }
        val failed = feeds.count { it.lastStatus == "FAILED" }
        MetaText("${feeds.size} kaynak · $ok hazır · $failed başarısız · ${feeds.size - ok - failed} bekliyor")
    }
}

@Composable
private fun RecentPanel(vm: CommandCenterViewModel, onNavigate: (Destination) -> Unit, modifier: Modifier) {
    val items by vm.recentLibrary.collectAsStateWithLifecycle()
    AtlasPanel(modifier) {
        SectionLabel("Son notlar")
        if (items.isEmpty()) EmptyHint("Kütüphane henüz boş.") else items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AtlasChip(item.type)
                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory, maxLines = 1)
            }
        }
        AtlasTextButton("Kütüphaneyi aç", { onNavigate(Destination.Library) })
    }
}

@Composable
private fun QuestionsPanel(vm: CommandCenterViewModel, onNavigate: (Destination) -> Unit, modifier: Modifier) {
    val questions by vm.openQuestions.collectAsStateWithLifecycle()
    AtlasPanel(modifier) {
        SectionLabel("Çözülmemiş sorular")
        if (questions.isEmpty()) EmptyHint("Açık soru yok.") else questions.forEach { q ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AtlasChip(QuestionStatus.fromName(q.status).label, highlight = true)
                Text(q.title, style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory, maxLines = 2)
            }
        }
        AtlasTextButton("Soru Motorunu aç", { onNavigate(Destination.Questions) })
    }
}

@Composable
private fun NotebookPanel(vm: CommandCenterViewModel, onNavigate: (Destination) -> Unit, modifier: Modifier) {
    val pages by vm.notebookCount.collectAsStateWithLifecycle()
    AtlasPanel(modifier, container = AtlasColors.Burgundy, borderColor = AtlasColors.BrassText.copy(alpha = 0.45f)) {
        SectionLabel("Defter · hızlı erişim")
        Text(
            if (pages == 0) "Henüz sayfa yok. S Pen ile ilk sayfanı aç." else "$pages sayfa kayıtlı.",
            style = MaterialTheme.typography.bodyMedium, color = AtlasColors.Ivory,
        )
        AtlasTextButton("Defteri aç", { onNavigate(Destination.Notebook) })
    }
}
