package com.atlas.app.feature.notebook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.atlas.app.data.db.StrokeEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

const val PAGE_W = 1000f
const val PAGE_H = 1414f

enum class Tool(val label: String) {
    FOUNTAIN("Dolma kalem"),
    BALLPOINT("Tükenmez"),
    PENCIL("Kurşun kalem"),
    HIGHLIGHTER("Fosforlu"),
    ERASER("Silgi"),
    LINE("Çizgi"),
    ARROW("Ok"),
    RECT("Dikdörtgen"),
    ELLIPSE("Elips"),
    ;

    val isShape: Boolean get() = this == LINE || this == ARROW || this == RECT || this == ELLIPSE

    companion object {
        fun fromName(name: String): Tool = entries.firstOrNull { it.name == name } ?: BALLPOINT
    }
}

enum class Paper(val label: String) {
    DOT("Noktalı"),
    GRID("Kareli"),
    LINED("Çizgili"),
    GRAPH("Grafik"),
    BLANK("Boş"),
    ;

    companion object {
        fun fromName(name: String): Paper = entries.firstOrNull { it.name == name } ?: DOT
    }
}

/** Sayfadaki bir çizgi. Noktalar [x, y, basınç] üçlüleri olarak [pts] içinde, ilk [n] nokta geçerlidir. */
class Ink(
    var id: Long,
    val tool: Tool,
    val color: Int,
    val width: Float,
    var pts: FloatArray,
    var n: Int = pts.size / 3,
) {
    fun add(x: Float, y: Float, p: Float) {
        if (n * 3 + 3 > pts.size) pts = pts.copyOf(max(pts.size * 2, 96))
        pts[n * 3] = x
        pts[n * 3 + 1] = y
        pts[n * 3 + 2] = p
        n++
    }

    fun x(i: Int) = pts[i * 3]
    fun y(i: Int) = pts[i * 3 + 1]
    fun p(i: Int) = pts[i * 3 + 2]
}

fun pack(ink: Ink): ByteArray {
    val b = ByteBuffer.allocate(ink.n * 3 * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until ink.n * 3) b.putFloat(ink.pts[i])
    return b.array()
}

fun unpack(bytes: ByteArray): FloatArray {
    val fb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    val a = FloatArray(fb.remaining())
    fb.get(a)
    return a
}

fun StrokeEntity.toInk() = Ink(id, Tool.fromName(tool), color, width, unpack(points))

fun Ink.toEntity(pageId: Long) = StrokeEntity(
    pageId = pageId, tool = tool.name, color = color, width = width, points = pack(this),
)

// ---------- Çizim ----------

private fun smoothPath(ink: Ink): Path {
    val path = Path()
    if (ink.n == 0) return path
    path.moveTo(ink.x(0), ink.y(0))
    if (ink.n == 1) {
        path.lineTo(ink.x(0) + 0.01f, ink.y(0))
        return path
    }
    for (i in 1 until ink.n - 1) {
        val mx = (ink.x(i) + ink.x(i + 1)) / 2f
        val my = (ink.y(i) + ink.y(i + 1)) / 2f
        path.quadraticBezierTo(ink.x(i), ink.y(i), mx, my)
    }
    path.lineTo(ink.x(ink.n - 1), ink.y(ink.n - 1))
    return path
}

/** Baskı/PDF için açık renkli mürekkebi beyaz kâğıtta okunur koyu bir tona çevirir. */
fun printColor(argb: Int, highlighter: Boolean): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    if (hsv[1] < 0.35f && hsv[2] > 0.7f) {
        return if (highlighter) Color(0xFFE8C547) else Color(0xFF1C1B1F)
    }
    if (!highlighter) hsv[2] = min(hsv[2], 0.62f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun DrawScope.drawInk(ink: Ink, print: Boolean = false) {
    if (ink.n == 0) return
    val color = if (print) printColor(ink.color, ink.tool == Tool.HIGHLIGHTER) else Color(ink.color)
    when (ink.tool) {
        Tool.FOUNTAIN -> {
            if (ink.n == 1) {
                drawCircle(color, radius = ink.width * 0.6f, center = Offset(ink.x(0), ink.y(0)))
                return
            }
            var prevW = ink.width * (0.35f + 1.15f * ink.p(0))
            for (i in 1 until ink.n) {
                val target = ink.width * (0.35f + 1.15f * ink.p(i))
                val w = prevW * 0.6f + target * 0.4f // basınç değişimini yumuşat
                drawLine(
                    color, Offset(ink.x(i - 1), ink.y(i - 1)), Offset(ink.x(i), ink.y(i)),
                    strokeWidth = w, cap = StrokeCap.Round,
                )
                prevW = w
            }
        }
        Tool.HIGHLIGHTER -> drawPath(
            smoothPath(ink), color.copy(alpha = 0.35f),
            style = Stroke(width = ink.width * 5f, cap = StrokeCap.Square, join = StrokeJoin.Round),
        )
        Tool.PENCIL -> drawPath(
            smoothPath(ink), color.copy(alpha = 0.85f),
            style = Stroke(width = max(1f, ink.width * 0.75f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        else -> drawPath(
            smoothPath(ink), color,
            style = Stroke(width = ink.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

fun DrawScope.drawPaper(paper: Paper, print: Boolean = false) {
    val bg = if (print) Color.White else Color(0xFF1E1C20)
    val minor = if (print) Color(0xFFD5D0CA) else Color(0xFF8D8179).copy(alpha = 0.22f)
    val major = if (print) Color(0xFFB8B2AA) else Color(0xFF8D8179).copy(alpha = 0.40f)
    drawRect(bg, size = Size(PAGE_W, PAGE_H))
    when (paper) {
        Paper.BLANK -> Unit
        Paper.DOT -> {
            var y = 40f
            while (y < PAGE_H) {
                var x = 40f
                while (x < PAGE_W) {
                    drawCircle(major, radius = 1.6f, center = Offset(x, y))
                    x += 40f
                }
                y += 40f
            }
        }
        Paper.GRID -> {
            var x = 40f
            while (x < PAGE_W) { drawLine(minor, Offset(x, 0f), Offset(x, PAGE_H), 1.2f); x += 40f }
            var y = 40f
            while (y < PAGE_H) { drawLine(minor, Offset(0f, y), Offset(PAGE_W, y), 1.2f); y += 40f }
        }
        Paper.LINED -> {
            var y = 96f
            while (y < PAGE_H) { drawLine(minor, Offset(0f, y), Offset(PAGE_W, y), 1.2f); y += 48f }
            drawLine(major, Offset(110f, 0f), Offset(110f, PAGE_H), 1.4f)
        }
        Paper.GRAPH -> {
            var i = 0
            var x = 0f
            while (x <= PAGE_W) {
                drawLine(if (i % 5 == 0) major else minor, Offset(x, 0f), Offset(x, PAGE_H), if (i % 5 == 0) 1.4f else 0.8f)
                x += 10f; i++
            }
            i = 0
            var y = 0f
            while (y <= PAGE_H) {
                drawLine(if (i % 5 == 0) major else minor, Offset(0f, y), Offset(PAGE_W, y), if (i % 5 == 0) 1.4f else 0.8f)
                y += 10f; i++
            }
        }
    }
}

// ---------- Silgi ve şekiller ----------

private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = bx - ax
    val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
    val cx = ax + t * dx
    val cy = ay + t * dy
    return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
}

fun Ink.hitBy(x: Float, y: Float, radius: Float): Boolean {
    val r = radius + width
    if (n == 1) return sqrt((x - x(0)) * (x - x(0)) + (y - y(0)) * (y - y(0))) <= r
    for (i in 1 until n) {
        if (distToSegment(x, y, x(i - 1), y(i - 1), x(i), y(i)) <= r) return true
    }
    return false
}

/** Şekil aracının iki köşesinden düz bir çizgi (Ink) üretir; kaydedilince normal bir kalem çizgisidir. */
fun shapeInk(tool: Tool, a: Offset, b: Offset, color: Int, width: Float): Ink {
    val ink = Ink(0, Tool.BALLPOINT, color, width, FloatArray(96), 0)
    fun pt(o: Offset) = ink.add(o.x, o.y, 0.5f)
    when (tool) {
        Tool.LINE -> { pt(a); pt(b) }
        Tool.ARROW -> {
            pt(a); pt(b)
            val ang = kotlin.math.atan2(b.y - a.y, b.x - a.x)
            val head = 22f + width * 2f
            val spread = (PI / 7).toFloat()
            pt(Offset(b.x - head * cos(ang - spread), b.y - head * sin(ang - spread)))
            pt(b)
            pt(Offset(b.x - head * cos(ang + spread), b.y - head * sin(ang + spread)))
        }
        Tool.RECT -> {
            pt(a); pt(Offset(b.x, a.y)); pt(b); pt(Offset(a.x, b.y)); pt(a)
        }
        Tool.ELLIPSE -> {
            val cx = (a.x + b.x) / 2f
            val cy = (a.y + b.y) / 2f
            val rx = kotlin.math.abs(b.x - a.x) / 2f
            val ry = kotlin.math.abs(b.y - a.y) / 2f
            val steps = 56
            for (i in 0..steps) {
                val t = (2 * PI * i / steps).toFloat()
                pt(Offset(cx + rx * cos(t), cy + ry * sin(t)))
            }
        }
        else -> Unit
    }
    return ink
}
