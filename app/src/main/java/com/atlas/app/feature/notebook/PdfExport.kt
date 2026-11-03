package com.atlas.app.feature.notebook

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.atlas.app.data.db.AtlasDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Defteri A4 PDF'e aktarır. Sayfa beyaz zeminde çizilir, açık renkli mürekkep okunur koyu tonlara çevrilir.
 * Yalnızca çizgiler ve kâğıt deseni aktarılır (ATLAS'ta metin kutusu/görsel yoktur).
 */
suspend fun exportNotebookPdf(context: Context, db: AtlasDatabase, notebookId: Long, uri: Uri): Result<Int> =
    withContext(Dispatchers.IO) {
        runCatching {
            val pages = db.notebookDao().getPages(notebookId)
            require(pages.isNotEmpty()) { "Defterde sayfa yok" }
            val doc = PdfDocument()
            try {
                pages.forEachIndexed { i, page ->
                    val strokes = db.strokeDao().getStrokes(page.id).map { it.toInk() }
                    val info = PdfDocument.PageInfo.Builder(595, 842, i + 1).create()
                    val pdfPage = doc.startPage(info)
                    val canvas = Canvas(pdfPage.canvas)
                    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(595f, 842f)) {
                        val k = 595f / PAGE_W
                        scale(k, k, pivot = Offset.Zero) {
                            drawPaper(Paper.fromName(page.paper), print = true)
                            strokes.forEach { drawInk(it, print = true) }
                        }
                    }
                    doc.finishPage(pdfPage)
                }
                val out = context.contentResolver.openOutputStream(uri) ?: error("Dosya açılamadı")
                out.use { doc.writeTo(it) }
            } finally {
                doc.close()
            }
            pages.size
        }
    }
