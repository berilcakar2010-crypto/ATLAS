package com.atlas.app.feature.notebook

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.atlas.app.data.db.NotebookEntity
import com.atlas.app.data.db.NotebookPageEntity
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.launch

private val inkColors = listOf(
    0xFFE7D8C4.toInt(), // fildişi
    0xFFC9A06A.toInt(), // pirinç
    0xFFC48A7A.toInt(), // kil
    0xFF9DB0A6.toInt(), // adaçayı
    0xFF8FA3B8.toInt(), // arduvaz
    0xFFB0728F.toInt(), // devetüyü-mor
)

@Composable
fun NotebookScreen() {
    var notebookId by rememberSaveable { mutableStateOf(-1L) }
    var startPageId by rememberSaveable { mutableStateOf(-1L) }
    if (notebookId < 0) {
        NotebookList(onOpen = { nb, page -> notebookId = nb; startPageId = page })
    } else {
        NotebookEditor(notebookId, startPageId, onBack = { notebookId = -1L; startPageId = -1L })
    }
}

@Composable
private fun NotebookList(onOpen: (Long, Long) -> Unit) {
    val app = rememberAtlasApp()
    val dao = app.database.notebookDao()
    val scope = rememberCoroutineScope()
    val notebooks by dao.observeNotebooks().collectAsStateWithLifecycle(emptyList())
    var title by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    val results by remember(query) {
        if (query.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList<NotebookPageEntity>()) else dao.searchPages(query.trim())
    }.collectAsStateWithLifecycle(emptyList())
    var toDelete by remember { mutableStateOf<NotebookEntity?>(null) }
    val nameById = notebooks.associate { it.id to it.title }

    ScreenFrame(Destination.Notebook.sectionLabel, "Araştırma defteri") {
        MetaText(
            "S Pen ile çizim, kâğıt türleri, şekil araçları, PDF'e aktarma. El yazısı metne ÇEVRİLMEZ; " +
                "bu yüzden arama yalnızca sayfa başlıklarında çalışır. Metin kutusu, görsel ve PDF üstüne not alma yoktur.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasField(title, { title = it }, "Yeni defter adı", Modifier.weight(1f), singleLine = true)
            AtlasButton("Defter oluştur", {
                val name = title.trim()
                title = ""
                scope.launch {
                    val nb = dao.insertNotebook(NotebookEntity(title = name))
                    val page = dao.insertPage(NotebookPageEntity(notebookId = nb, pageIndex = 0))
                    onOpen(nb, page)
                }
            }, enabled = title.isNotBlank())
        }
        AtlasField(query, { query = it }, "Sayfa başlıklarında ara", Modifier.fillMaxWidth(), singleLine = true)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (query.isNotBlank()) {
                item { SectionLabel("Arama sonuçları · ${results.size}") }
                items(results, key = { "r${it.id}" }) { p ->
                    AtlasPanel(Modifier.fillMaxWidth().clickable { onOpen(p.notebookId, p.id) }) {
                        Text(p.title.ifBlank { "Başlıksız sayfa" }, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                        MetaText("${nameById[p.notebookId] ?: "Defter"} · sayfa ${p.pageIndex + 1}")
                    }
                }
            }
            item { SectionLabel("Defterlerim · ${notebooks.size}") }
            if (notebooks.isEmpty()) item { EmptyHint("Henüz defter yok. Yukarıdan bir tane oluştur.") }
            items(notebooks, key = { it.id }) { nb ->
                AtlasPanel(Modifier.fillMaxWidth()) {
                    Text(nb.title, style = MaterialTheme.typography.titleMedium, color = AtlasColors.Ivory)
                    MetaText("Oluşturuldu: ${Format.timestamp(nb.createdAt)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasOutlinedButton("Aç", {
                            scope.launch {
                                val first = dao.getPages(nb.id).firstOrNull()?.id
                                    ?: dao.insertPage(NotebookPageEntity(notebookId = nb.id, pageIndex = 0))
                                onOpen(nb.id, first)
                            }
                        })
                        AtlasTextButton("Sil", { toDelete = nb })
                    }
                }
            }
        }
    }
    toDelete?.let { nb ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Defteri sil?") },
            text = { Text("'${nb.title}' ve içindeki tüm sayfalar ile çizimler kalıcı olarak silinir. Bu geri alınamaz.") },
            confirmButton = { AtlasTextButton("Sil", { scope.launch { dao.deleteNotebook(nb.id) }; toDelete = null }) },
            dismissButton = { AtlasTextButton("Vazgeç", { toDelete = null }) },
        )
    }
}

@Composable
private fun NotebookEditor(notebookId: Long, startPageId: Long, onBack: () -> Unit) {
    val app = rememberAtlasApp()
    val context = LocalContext.current
    val db = app.database
    val dao = db.notebookDao()
    val scope = rememberCoroutineScope()
    val state = remember { EditorState(db, app.appScope) }

    val notebooks by dao.observeNotebooks().collectAsStateWithLifecycle(emptyList())
    val pages by dao.observePages(notebookId).collectAsStateWithLifecycle(emptyList())
    var pageId by rememberSaveable { mutableStateOf(startPageId) }
    val page = pages.firstOrNull { it.id == pageId } ?: pages.firstOrNull()

    var tool by rememberSaveable { mutableStateOf(Tool.FOUNTAIN) }
    var colorIdx by rememberSaveable { mutableStateOf(0) }
    var widthIdx by rememberSaveable { mutableStateOf(1) }
    var finger by rememberSaveable { mutableStateOf(false) }
    var confirmDeletePage by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(page?.id) { page?.let { state.load(it.id) } }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            scope.launch {
                val r = exportNotebookPdf(context, db, notebookId, uri)
                val msg = r.fold({ "PDF kaydedildi ($it sayfa)" }, { "PDF oluşturulamadı: ${it.message}" })
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val nbTitle = notebooks.firstOrNull { it.id == notebookId }?.title ?: "Defter"
    ScreenFrame(Destination.Notebook.sectionLabel, nbTitle) {
        // Üst şerit: gezinme ve sayfa işlemleri
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasOutlinedButton("‹ Defterler", onBack)
            pages.forEachIndexed { i, p ->
                AtlasChip(
                    "${i + 1}", highlight = p.id == page?.id,
                    modifier = Modifier.clickable { pageId = p.id }.padding(2.dp),
                )
            }
            AtlasTextButton("+ Sayfa", {
                scope.launch {
                    val idx = dao.maxPageIndex(notebookId) + 1
                    pageId = dao.insertPage(NotebookPageEntity(notebookId = notebookId, pageIndex = idx, paper = page?.paper ?: "DOT"))
                }
            })
            AtlasTextButton("Sayfayı sil", { confirmDeletePage = true })
            AtlasTextButton("PDF", { exporter.launch("${nbTitle.replace(Regex("[^\\p{L}\\p{N} _-]"), "_")}.pdf") })
        }
        if (page != null) {
            var pageTitle by remember(page.id) { mutableStateOf(page.title) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AtlasField(
                    pageTitle, { pageTitle = it; scope.launch { dao.setPageTitle(page.id, it, System.currentTimeMillis()) } },
                    "Sayfa başlığı (aramada kullanılır)", Modifier.weight(1f), singleLine = true,
                )
                FilterRow(
                    Paper.entries.map { it.name to it.label }, page.paper,
                    { scope.launch { dao.setPagePaper(page.id, it, System.currentTimeMillis()) } },
                )
            }
        }
        // Araç çubuğu
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterRow(Tool.entries.map { it.name to it.label }, tool.name, { tool = Tool.fromName(it) })
            inkColors.forEachIndexed { i, c ->
                Box(
                    Modifier.size(32.dp).background(Color(c), CircleShape)
                        .border(if (i == colorIdx) 3.dp else 1.dp, if (i == colorIdx) AtlasColors.Ivory else AtlasColors.Line, CircleShape)
                        .clickable { colorIdx = i },
                )
            }
            listOf("İnce", "Orta", "Kalın").forEachIndexed { i, label ->
                AtlasChip(label, highlight = i == widthIdx, modifier = Modifier.clickable { widthIdx = i }.padding(2.dp))
            }
            AtlasTextButton("↶ Geri al", { if (state.canUndo) state.undo() })
            AtlasTextButton("↷ Yinele", { if (state.canRedo) state.redo() })
            AtlasTextButton("Temizle", { confirmClear = true })
            AtlasChip(
                if (finger) "Parmakla çiz: açık" else "Parmak: kaydır",
                highlight = finger,
                modifier = Modifier.clickable { finger = !finger }.padding(2.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (page == null) {
                EmptyHint("Sayfa yükleniyor…")
            } else {
                InkCanvas(
                    state = state,
                    paper = Paper.fromName(page.paper),
                    tool = tool,
                    color = inkColors[colorIdx],
                    width = listOf(2.2f, 4f, 7f)[widthIdx],
                    fingerDraws = finger,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        MetaText("S Pen çizer, parmak kaydırır/yakınlaştırır. Şekil araçları düz çizgiler üretir (tanıma değil, geometri). Otomatik kaydedilir.")
    }

    if (confirmDeletePage && page != null) {
        AlertDialog(
            onDismissRequest = { confirmDeletePage = false },
            title = { Text("Sayfayı sil?") },
            text = { Text(if (pages.size <= 1) "Defterde tek sayfa var; silinemez. Yerine 'Temizle'yi kullanabilirsin." else "Bu sayfa ve çizimleri kalıcı olarak silinir.") },
            confirmButton = {
                AtlasTextButton("Sil", {
                    if (pages.size > 1) {
                        val next = pages.firstOrNull { it.id != page.id }?.id ?: -1L
                        scope.launch { dao.deletePage(page.id) }
                        pageId = next
                    }
                    confirmDeletePage = false
                })
            },
            dismissButton = { AtlasTextButton("Vazgeç", { confirmDeletePage = false }) },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Sayfa temizlensin mi?") },
            text = { Text("Tüm çizimler kaldırılır. 'Geri al' ile hemen geri getirebilirsin.") },
            confirmButton = { AtlasTextButton("Temizle", { state.clearPage(); confirmClear = false }) },
            dismissButton = { AtlasTextButton("Vazgeç", { confirmClear = false }) },
        )
    }
}
