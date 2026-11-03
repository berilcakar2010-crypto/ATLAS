package com.atlas.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.data.db.AtlasDatabase
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.feature.common.openUrl
import com.atlas.app.feature.common.relativeTime
import kotlinx.coroutines.launch

/** Kaydedilen her şeyin tek listede görünümü: haber, makale, sözcük, not, gözlem. */
private data class SavedItem(
    val key: String,
    val kind: String, // article | paper | word | note | observation
    val kindLabel: String,
    val title: String,
    val meta: String,
    val body: String,
    val userNote: String,
    val url: String?,
    val at: Long,
)

@Composable
fun SavedPanel(db: AtlasDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val articles by db.articleDao().observeSaved().collectAsStateWithLifecycle(emptyList())
    val papers by db.paperDao().observeSaved().collectAsStateWithLifecycle(emptyList())
    val words by db.vocabDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val notes by db.libraryDao().observeAll().collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }

    val items = remember(articles, papers, words, notes) {
        buildList {
            articles.forEach {
                add(SavedItem("a${it.id}", "article", "Haber", it.title, "${it.sourceName} · ${relativeTime(it.publishedAt)}" + if (it.tags.isNotBlank()) " · ${it.tags}" else "", it.summary, it.note, it.url, it.savedAt))
            }
            papers.forEach {
                add(SavedItem("p${it.id}", "paper", if (it.source == "ARXIV") "Makale · arXiv" else "Makale · PubMed", it.title, "${it.authors.take(90)} · ${it.journal} · ${relativeTime(it.publishedAt)}", it.abstractText, it.annotation, it.url, it.savedAt))
            }
            words.forEach {
                add(SavedItem("w${it.id}", "word", "Sözcük", it.word, if (it.context.isNotBlank()) "Kaynak: ${it.context}" else "", it.meaning + if (it.example.isNotBlank()) "\n“${it.example}”" else "", "", null, it.createdAt))
            }
            notes.forEach {
                val obs = it.type == "OBSERVATION"
                add(SavedItem("n${it.id}", if (obs) "observation" else "note", if (obs) "Gözlem" else "Not", it.title, it.source?.let { s -> "Kaynak/yöntem: $s" }.orEmpty(), it.body, "", it.url, it.createdAt))
            }
        }.sortedByDescending { it.at }
    }
    val counts = items.groupingBy { it.kind }.eachCount()
    val shown = items.filter {
        (filter == "all" || it.kind == filter) &&
            (query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true) ||
                it.meta.contains(query, true) || it.userNote.contains(query, true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterRow(
            listOf(
                "all" to "Hepsi · ${items.size}",
                "article" to "Haberler · ${counts["article"] ?: 0}",
                "paper" to "Makaleler · ${counts["paper"] ?: 0}",
                "word" to "Sözcükler · ${counts["word"] ?: 0}",
                "note" to "Notlar · ${counts["note"] ?: 0}",
                "observation" to "Gözlemler · ${counts["observation"] ?: 0}",
            ),
            filter, { filter = it },
        )
        AtlasField(query, { query = it }, "Tüm kayıtlılarda ara", Modifier.fillMaxWidth(), singleLine = true)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shown.isEmpty()) {
                item { EmptyHint("Burada gösterilecek kayıt yok. Haber/makalede 'Kaydet'e dokun, Sözlük'ten sözcük ekle veya not yaz.") }
            }
            items(shown, key = { it.key }) { it ->
                AtlasPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasChip(it.kindLabel, highlight = it.kind == "word")
                        MetaText("kaydedildi " + relativeTime(it.at))
                    }
                    Text(it.title, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                    if (it.meta.isNotBlank()) MetaText(it.meta)
                    if (it.body.isNotBlank()) Text(it.body, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                    if (it.userNote.isNotBlank()) Text("Notum: ${it.userNote}", style = MaterialTheme.typography.bodyMedium, color = AtlasColors.BrassText)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        it.url?.let { u -> AtlasOutlinedButton("Aç", { openUrl(context, u) }) }
                        if (it.kind == "article" || it.kind == "paper") {
                            AtlasTextButton("Kaydı kaldır", {
                                val id = it.key.drop(1).toLong()
                                scope.launch {
                                    if (it.kind == "article") db.articleDao().setSaved(id, false, 0)
                                    else db.paperDao().setSaved(id, false, 0)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}
