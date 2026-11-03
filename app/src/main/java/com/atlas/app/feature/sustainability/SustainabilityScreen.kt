package com.atlas.app.feature.sustainability

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlas.app.AtlasApp
import com.atlas.app.core.design.AtlasButton
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.core.util.Format
import com.atlas.app.data.db.ArticleEntity
import com.atlas.app.data.db.LibraryItemEntity
import com.atlas.app.data.feed.Category
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.SyncBar
import com.atlas.app.feature.intelligence.ArticleCard
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val OBSERVATION = "OBSERVATION"

class SustainabilityViewModel(private val app: AtlasApp) : ViewModel() {
    private val db = app.database
    val articles: StateFlow<List<ArticleEntity>> = db.articleDao()
        .observeByCategories(
            listOf(Category.CLIMATE.name, Category.SUSTAINABILITY.name),
            System.currentTimeMillis() - 30L * 24 * 3600_000,
            120,
        )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val observations: StateFlow<List<LibraryItemEntity>> = db.libraryDao().observeByType(OBSERVATION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addObservation(title: String, body: String, source: String) {
        viewModelScope.launch {
            db.libraryDao().insert(
                LibraryItemEntity(type = OBSERVATION, title = title.trim(), body = body.trim(), source = source.trim().ifEmpty { null }),
            )
        }
    }

    fun deleteObservation(id: Long) {
        viewModelScope.launch { db.libraryDao().delete(id) }
    }

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
fun SustainabilityScreen() {
    val app = rememberAtlasApp()
    val vm: SustainabilityViewModel = viewModel(factory = viewModelFactory { initializer { SustainabilityViewModel(app) } })
    val articles by vm.articles.collectAsStateWithLifecycle()
    val observations by vm.observations.collectAsStateWithLifecycle()

    ScreenFrame(Destination.Sustainability.sectionLabel, "Sürdürülebilirlik ve iklim izleme") {
        SyncBar(app)
        MetaText(
            "Son 30 günün iklim ve sürdürülebilirlik haberleri (Carbon Brief, Yale e360, Mongabay, Our World in Data vb.). " +
                "ATLAS sayı veya istatistik üretmez; yalnızca kaynağın yayımladığını ve senin gözlemlerini gösterir.",
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ObservationForm(vm, observations)
            }
            item { SectionLabel("Kaynaklardan gelenler · ${articles.size}") }
            if (articles.isEmpty()) {
                item { EmptyHint("Henüz bu başlıkta haber yok. Eşitleme sonrası burada görünür.") }
            }
            items(articles, key = { it.id }) { a ->
                ArticleCard(a, { vm.toggleSaved(a) }, { n, t -> vm.saveNote(a, n, t) })
            }
        }
    }
}

@Composable
private fun ObservationForm(vm: SustainabilityViewModel, observations: List<LibraryItemEntity>) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    AtlasPanel(Modifier.fillMaxWidth()) {
        SectionLabel("Kendi gözlemlerim · zaman çizelgesi")
        MetaText("Yerel bir gözlem, ölçüm veya fark ettiğin değişimi tarihiyle kaydet. Kaynak gösterebilirsen yaz.")
        AtlasField(title, { title = it }, "Gözlem başlığı", Modifier.fillMaxWidth(), singleLine = true)
        AtlasField(body, { body = it }, "Ne gördün / ölçtün?", Modifier.fillMaxWidth(), minLines = 2)
        AtlasField(source, { source = it }, "Kaynak veya yöntem (isteğe bağlı)", Modifier.fillMaxWidth(), singleLine = true)
        AtlasButton("Gözlemi kaydet", {
            vm.addObservation(title, body, source)
            title = ""; body = ""; source = ""
        }, enabled = title.isNotBlank())
        if (observations.isEmpty()) {
            EmptyHint("Henüz gözlem yok.")
        }
        observations.forEach { o ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaText(Format.timestamp(o.createdAt), Modifier.weight(0.35f))
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(o.title, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                    if (o.body.isNotBlank()) Text(o.body, style = MaterialTheme.typography.bodyMedium)
                    o.source?.let { MetaText("Kaynak/yöntem: $it") }
                }
                AtlasTextButton("Sil", { vm.deleteObservation(o.id) })
            }
        }
    }
}
