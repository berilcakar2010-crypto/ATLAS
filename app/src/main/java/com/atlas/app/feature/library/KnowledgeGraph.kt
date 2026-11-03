package com.atlas.app.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.data.db.AtlasDatabase
import com.atlas.app.feature.common.openUrl
import com.atlas.app.feature.common.relativeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

enum class NodeType(val label: String, val color: Color) {
    NOTE("Not", AtlasColors.Ivory),
    ARTICLE("Haber", AtlasColors.Brass),
    PAPER("Makale", Color(0xFF9DB0A6)),
    QUESTION("Soru", Color(0xFFB0728F)),
}

data class GNode(
    val key: String,
    val type: NodeType,
    val title: String,
    val tokens: Set<String>,
    val url: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
)

data class Graph(val nodes: List<GNode>, val edges: List<Pair<Int, Int>>)

private fun tokensOf(csv: String): Set<String> =
    csv.split(',', ';').map { it.trim().lowercase() }.filter { it.length >= 2 }.toSet()

/**
 * Kenar kuralı (açık ve basit): iki düğüm, ortak bir etiketi/konusu varsa VEYA birinin etiketi
 * diğerinin başlığında geçiyorsa bağlanır. Notların etiketi yoktur; yalnızca başlıklarındaki eşleşmelerle bağlanır.
 * Bu bir "anlamsal benzerlik" değil, yalnızca metin eşleşmesidir.
 */
fun buildEdges(nodes: List<GNode>): List<Pair<Int, Int>> {
    val edges = mutableListOf<Pair<Int, Int>>()
    for (i in nodes.indices) for (j in i + 1 until nodes.size) {
        val a = nodes[i]
        val b = nodes[j]
        val shared = a.tokens.any { it in b.tokens }
        val inTitle = a.tokens.any { b.title.lowercase().contains(it) } || b.tokens.any { a.title.lowercase().contains(it) }
        if (shared || inTitle) edges += i to j
    }
    return edges
}

/** Kuvvet yönelimli yerleşim (Fruchterman–Reingold), 1000×700 alanda. */
fun layout(nodes: List<GNode>, edges: List<Pair<Int, Int>>): List<GNode> {
    if (nodes.isEmpty()) return nodes
    val w = 1000f
    val h = 700f
    val rnd = Random(7)
    val px = FloatArray(nodes.size) { w * (0.2f + 0.6f * rnd.nextFloat()) }
    val py = FloatArray(nodes.size) { h * (0.2f + 0.6f * rnd.nextFloat()) }
    val k = sqrt(w * h / nodes.size) * 0.8f
    var temp = w / 8f
    repeat(250) {
        val dx = FloatArray(nodes.size)
        val dy = FloatArray(nodes.size)
        for (i in nodes.indices) for (j in i + 1 until nodes.size) {
            var ddx = px[i] - px[j]
            var ddy = py[i] - py[j]
            var d = sqrt(ddx * ddx + ddy * ddy)
            if (d < 0.01f) { ddx = rnd.nextFloat() - 0.5f; ddy = rnd.nextFloat() - 0.5f; d = 0.5f }
            val f = k * k / d
            dx[i] += ddx / d * f; dy[i] += ddy / d * f
            dx[j] -= ddx / d * f; dy[j] -= ddy / d * f
        }
        for ((a, b) in edges) {
            val ddx = px[a] - px[b]
            val ddy = py[a] - py[b]
            val d = max(sqrt(ddx * ddx + ddy * ddy), 0.01f)
            val f = d * d / k
            dx[a] -= ddx / d * f; dy[a] -= ddy / d * f
            dx[b] += ddx / d * f; dy[b] += ddy / d * f
        }
        for (i in nodes.indices) {
            // merkeze hafif çekim: bağlantısız düğümler uzağa kaçmasın
            dx[i] += (w / 2 - px[i]) * 0.02f
            dy[i] += (h / 2 - py[i]) * 0.02f
            val d = max(sqrt(dx[i] * dx[i] + dy[i] * dy[i]), 0.01f)
            val m = min(d, temp)
            px[i] = (px[i] + dx[i] / d * m).coerceIn(30f, w - 30f)
            py[i] = (py[i] + dy[i] / d * m).coerceIn(30f, h - 30f)
        }
        temp *= 0.98f
    }
    return nodes.mapIndexed { i, n -> n.copy(x = px[i], y = py[i]) }
}

@Composable
fun KnowledgeGraphPanel(db: AtlasDatabase) {
    val context = LocalContext.current
    val notes by db.libraryDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val articles by db.articleDao().observeSaved().collectAsStateWithLifecycle(emptyList())
    val papers by db.paperDao().observeSaved().collectAsStateWithLifecycle(emptyList())
    val questions by db.questionDao().observeAll().collectAsStateWithLifecycle(emptyList())

    val rawNodes = remember(notes, articles, papers, questions) {
        buildList {
            notes.filter { it.type != "OBSERVATION" }.take(60).forEach {
                add(GNode("n${it.id}", NodeType.NOTE, it.title, emptySet(), it.url))
            }
            articles.take(60).forEach { add(GNode("a${it.id}", NodeType.ARTICLE, it.title, tokensOf(it.tags), it.url)) }
            papers.take(60).forEach { add(GNode("p${it.id}", NodeType.PAPER, it.title, tokensOf(it.tags), it.url)) }
            questions.take(60).forEach { add(GNode("q${it.id}", NodeType.QUESTION, it.title, tokensOf(it.subject), null)) }
        }
    }
    var graph by remember { mutableStateOf(Graph(emptyList(), emptyList())) }
    LaunchedEffect(rawNodes) {
        graph = withContext(Dispatchers.Default) {
            val edges = buildEdges(rawNodes)
            Graph(layout(rawNodes, edges), edges)
        }
    }
    var selected by remember { mutableStateOf<Int?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val measurer = rememberTextMeasurer()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NodeType.entries.forEach { AtlasChip(it.label, highlight = false) }
        }
        MetaText(
            "Düğümler: notların, kaydettiğin haber/makaleler ve soruların. Kenar kuralı: ortak etiket/konu veya bir etiketin " +
                "diğer düğümün başlığında geçmesi. Bu anlamsal analiz değil, yalnızca metin eşleşmesidir. " +
                "Etiketi olmayan düğümler bağlantısız kalır; haber/makalelere etiket ekledikçe grafik zenginleşir.",
        )
        if (graph.nodes.isEmpty()) {
            EmptyHint("Grafik için henüz düğüm yok. Not, soru ekle veya haber/makale kaydet.")
        } else {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, AtlasColors.Line)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, p, z, _ ->
                            scale = (scale * z).coerceIn(0.5f, 4f)
                            pan += p
                        }
                    }
                    .pointerInput(graph) {
                        detectTapGestures { tap ->
                            val sx = size.width / 1000f
                            val sy = size.height / 700f
                            val s = min(sx, sy) * scale
                            var best = -1
                            var bestD = 40f * 40f
                            graph.nodes.forEachIndexed { i, n ->
                                val x = n.x * s + pan.x
                                val y = n.y * s + pan.y
                                val d = (x - tap.x) * (x - tap.x) + (y - tap.y) * (y - tap.y)
                                if (d < bestD) { bestD = d; best = i }
                            }
                            selected = if (best >= 0) best else null
                        }
                    },
            ) {
                val s = min(size.width / 1000f, size.height / 700f) * scale
                fun pos(n: GNode) = Offset(n.x * s + pan.x, n.y * s + pan.y)
                val sel = selected
                val neighbors = if (sel == null) emptySet() else graph.edges.mapNotNull {
                    when (sel) { it.first -> it.second; it.second -> it.first; else -> null }
                }.toSet()
                graph.edges.forEach { (a, b) ->
                    val hot = sel != null && (a == sel || b == sel)
                    drawLine(
                        color = if (hot) AtlasColors.Brass else AtlasColors.Taupe.copy(alpha = 0.35f),
                        start = pos(graph.nodes[a]), end = pos(graph.nodes[b]),
                        strokeWidth = if (hot) 2.5f else 1f,
                    )
                }
                graph.nodes.forEachIndexed { i, n ->
                    val c = pos(n)
                    drawCircle(n.type.color, radius = if (i == sel) 11f else 7f, center = c)
                    if (i == sel) drawCircle(AtlasColors.Ivory, radius = 15f, center = c, style = Stroke(2f))
                }
                if (sel != null) {
                    (neighbors + sel).forEach { i ->
                        val n = graph.nodes[i]
                        val c = pos(n)
                        val layoutResult = measurer.measure(
                            n.title,
                            style = TextStyle(color = AtlasColors.Ivory, fontSize = 11.sp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            constraints = Constraints(maxWidth = 190),
                        )
                        drawText(layoutResult, topLeft = Offset(c.x + 12f, c.y - 8f))
                    }
                }
            }
            val sel = selected?.let { graph.nodes.getOrNull(it) }
            if (sel != null) {
                AtlasPanel(Modifier.fillMaxWidth()) {
                    AtlasChip(sel.type.label, highlight = true)
                    Text(sel.title, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                    if (sel.tokens.isNotEmpty()) MetaText("Etiket/konu: " + sel.tokens.joinToString(", "))
                    sel.url?.let { u -> AtlasOutlinedButton("Kaynağı aç", { openUrl(context, u) }) }
                }
            } else {
                MetaText("Bir düğüme dokun. İki parmakla yakınlaştır/kaydır.")
            }
        }
    }
}

/** Kaydedilen haber ve makalelerin listesi. */
@Composable
fun SavedPanel(db: AtlasDatabase) {
    val context = LocalContext.current
    val articles by db.articleDao().observeSaved().collectAsStateWithLifecycle(emptyList())
    val papers by db.paperDao().observeSaved().collectAsStateWithLifecycle(emptyList())
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (articles.isEmpty() && papers.isEmpty()) {
            item { EmptyHint("Henüz kaydedilmiş haber veya makale yok. Daily Intelligence ve Research Radar'da 'Kaydet'e dokun.") }
        }
        if (papers.isNotEmpty()) item { SectionLabel("Makaleler · ${papers.size}") }
        items(papers, key = { "p${it.id}" }) { p ->
            AtlasPanel(Modifier.fillMaxWidth()) {
                AtlasChip(if (p.source == "ARXIV") "arXiv" else "PubMed")
                Text(p.title, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                MetaText("${p.authors.take(100)} · ${p.journal} · ${relativeTime(p.publishedAt)}")
                if (p.annotation.isNotBlank()) Text("Notum: ${p.annotation}", style = MaterialTheme.typography.bodyMedium)
                AtlasOutlinedButton("Aç", { openUrl(context, p.url) })
            }
        }
        if (articles.isNotEmpty()) item { SectionLabel("Haberler · ${articles.size}") }
        items(articles, key = { "a${it.id}" }) { a ->
            AtlasPanel(Modifier.fillMaxWidth()) {
                AtlasChip(a.sourceName)
                Text(a.title, style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
                MetaText(relativeTime(a.publishedAt) + if (a.tags.isNotBlank()) " · ${a.tags}" else "")
                if (a.note.isNotBlank()) Text("Notum: ${a.note}", style = MaterialTheme.typography.bodyMedium)
                AtlasOutlinedButton("Aç", { openUrl(context, a.url) })
            }
        }
    }
}
