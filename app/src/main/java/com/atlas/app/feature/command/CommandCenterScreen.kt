package com.atlas.app.feature.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
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
import com.atlas.app.data.db.AtlasDatabase
import com.atlas.app.data.db.LibraryItemEntity
import com.atlas.app.data.db.QuestionEntity
import com.atlas.app.data.db.QuestionStatus
import com.atlas.app.rememberAtlasApp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CommandCenterViewModel(db: AtlasDatabase) : ViewModel() {
    val recentLibrary: StateFlow<List<LibraryItemEntity>> = db.libraryDao().observeRecent(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val openQuestions: StateFlow<List<QuestionEntity>> = db.questionDao().observeOpen(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Takip edilecek haber alanları (Phase 4'te kaynaklar bağlanacak). */
private val PlannedAreas = listOf(
    "Bilim", "Hesaplamalı nörobilim", "Fizik", "Kozmoloji", "Biyoloji", "Teknoloji",
    "Yapay zekâ", "Sürdürülebilirlik", "İklim ve çevre", "Siyaset ve uluslararası ilişkiler",
    "Ekonomi", "Felsefe", "Edebiyat", "Sosyoloji",
)

/** Başlangıç araştırma ilgi alanları (brief'ten). */
private val ResearchTopics = listOf(
    "Hesaplamalı nörobilim",
    "Kuramsal nörobilim",
    "Sinaptik plastisite",
    "Kısa süreli sinaptik plastisite",
    "Hodgkin–Huxley modelleri",
    "Tsodyks–Markram modelleri",
    "Sinirsel kodlama",
    "Sinirsel dinamikler",
)

private val DateFormatter = DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy", Format.turkish)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Format.turkish)

@Composable
fun CommandCenterScreen(onNavigate: (Destination) -> Unit) {
    val app = rememberAtlasApp()
    val vm: CommandCenterViewModel = viewModel(
        factory = viewModelFactory { initializer { CommandCenterViewModel(app.database) } },
    )
    val recent by vm.recentLibrary.collectAsStateWithLifecycle()
    val openQuestions by vm.openQuestions.collectAsStateWithLifecycle()

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
            MetaText(now.format(DateFormatter).uppercase(Format.turkish))
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 860.dp
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        BriefingPanel(Modifier.weight(2f).fillMaxHeight())
                        TopicsPanel(Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        RecentPanel(recent, onNavigate, Modifier.weight(1f).fillMaxHeight())
                        QuestionsPanel(openQuestions, onNavigate, Modifier.weight(1f).fillMaxHeight())
                        NotebookPanel(onNavigate, Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    BriefingPanel(Modifier.fillMaxWidth())
                    TopicsPanel(Modifier.fillMaxWidth())
                    RecentPanel(recent, onNavigate, Modifier.fillMaxWidth())
                    QuestionsPanel(openQuestions, onNavigate, Modifier.fillMaxWidth())
                    NotebookPanel(onNavigate, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BriefingPanel(modifier: Modifier) {
    AtlasPanel(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Günlük bülten")
            AtlasChip("Uygulanmadı · Phase 4", highlight = true)
        }
        Text(
            text = "Haber kaynakları henüz bağlı değil. RSS, arXiv ve PubMed bağlantıları Phase 4'te eklenecek. " +
                "O zamana kadar burada uydurma haber veya özet gösterilmeyecek.",
            style = MaterialTheme.typography.bodyLarge,
            color = AtlasColors.Ivory,
        )
        HairlineDivider()
        SectionLabel("Takip edilecek alanlar", color = AtlasColors.TaupeText)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlannedAreas.forEach { AtlasChip(it) }
        }
    }
}

@Composable
private fun TopicsPanel(modifier: Modifier) {
    AtlasPanel(modifier) {
        SectionLabel("Aktif araştırma konuları")
        ResearchTopics.forEach { topic ->
            Text(
                text = topic,
                style = MaterialTheme.typography.bodyMedium,
                color = AtlasColors.Ivory,
            )
        }
        MetaText("Takip ve yeni makale sayaçları Research Radar ile (Phase 4).")
    }
}

@Composable
private fun RecentPanel(
    items: List<LibraryItemEntity>,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier,
) {
    AtlasPanel(modifier) {
        SectionLabel("Son kaydedilenler")
        if (items.isEmpty()) {
            EmptyHint("Kütüphane henüz boş.")
        } else {
            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AtlasChip(item.type)
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtlasColors.Ivory,
                        maxLines = 1,
                    )
                }
            }
        }
        AtlasTextButton("Kütüphaneyi aç", onClick = { onNavigate(Destination.Library) })
    }
}

@Composable
private fun QuestionsPanel(
    questions: List<QuestionEntity>,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier,
) {
    AtlasPanel(modifier) {
        SectionLabel("Çözülmemiş sorular")
        if (questions.isEmpty()) {
            EmptyHint("Açık soru yok.")
        } else {
            questions.forEach { question ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AtlasChip(QuestionStatus.fromName(question.status).label, highlight = true)
                    Text(
                        text = question.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtlasColors.Ivory,
                        maxLines = 2,
                    )
                }
            }
        }
        AtlasTextButton("Soru Motorunu aç", onClick = { onNavigate(Destination.Questions) })
    }
}

@Composable
private fun NotebookPanel(onNavigate: (Destination) -> Unit, modifier: Modifier) {
    AtlasPanel(
        modifier = modifier,
        container = AtlasColors.Burgundy,
        borderColor = AtlasColors.BrassText.copy(alpha = 0.45f),
    ) {
        SectionLabel("Defter · hızlı erişim")
        AtlasChip("Uygulanmadı · Phase 5", highlight = true)
        Text(
            text = "S Pen defteri henüz yok. Çizim, sayfa yönetimi ve PDF aktarımı Phase 5'te gelecek.",
            style = MaterialTheme.typography.bodyMedium,
            color = AtlasColors.Ivory,
        )
        AtlasTextButton("Modül sayfası", onClick = { onNavigate(Destination.Notebook) })
    }
}
