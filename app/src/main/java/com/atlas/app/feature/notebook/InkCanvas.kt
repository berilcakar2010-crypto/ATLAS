package com.atlas.app.feature.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.atlas.app.core.design.AtlasColors

/**
 * Çizim yüzeyi. S Pen / kalem çizer; parmak (varsayılan) yalnızca kaydırır ve yakınlaştırır.
 * Avuç içi reddi: kalem temasından sonra ~600 ms içindeki veya kalem basılıyken gelen parmak dokunuşları yok sayılır.
 * Bu bir sezgisel yöntemdir; her cihazda kusursuz olduğu iddia edilmez.
 */
@Composable
fun InkCanvas(
    state: EditorState,
    paper: Paper,
    tool: Tool,
    color: Int,
    width: Float,
    fingerDraws: Boolean,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    val toolState by rememberUpdatedState(tool)
    val colorState by rememberUpdatedState(color)
    val widthState by rememberUpdatedState(width)
    val fingerState by rememberUpdatedState(fingerDraws)

    // Sayfa değişince ya da boyut belli olunca sayfayı genişliğe sığdır.
    LaunchedEffect(state.pageId, size) {
        if (size.width > 0) {
            val s = size.width / PAGE_W * 0.96f
            scale = s
            pan = Offset((size.width - PAGE_W * s) / 2f, 12f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, AtlasColors.Line)
            .onSizeChanged { size = it }
            .pointerInput(state) {
                var lastStylusMs = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var event: androidx.compose.ui.input.pointer.PointerEvent? = null
                    var drawing = false
                    var erased = mutableListOf<Ink>()
                    var shapeStart: Offset? = null
                    var activeTool = toolState
                    var sawMultiTouch = false
                    var stylusDown = false

                    fun logical(o: Offset) = Offset((o.x - pan.x) / scale, (o.y - pan.y) / scale)

                    fun begin(pos: Offset, pressure: Float, eraserButton: Boolean) {
                        activeTool = if (eraserButton) Tool.ERASER else toolState
                        val p = logical(pos)
                        drawing = true
                        erased = mutableListOf()
                        when {
                            activeTool == Tool.ERASER -> erased.addAll(state.eraseAt(p.x, p.y, 14f / scale))
                            activeTool.isShape -> {
                                shapeStart = p
                                state.live = shapeInk(activeTool, p, p, colorState, widthState)
                            }
                            else -> {
                                val ink = Ink(0, activeTool, colorState, widthState, FloatArray(192), 0)
                                ink.add(p.x, p.y, pressure)
                                state.live = ink
                            }
                        }
                        state.liveVersion++
                    }

                    fun extend(pos: Offset, pressure: Float) {
                        if (!drawing) return
                        val p = logical(pos)
                        when {
                            activeTool == Tool.ERASER -> erased.addAll(state.eraseAt(p.x, p.y, 14f / scale))
                            activeTool.isShape -> {
                                val start = shapeStart ?: return
                                state.live = shapeInk(activeTool, start, p, colorState, widthState)
                            }
                            else -> {
                                val ink = state.live ?: return
                                val dx = p.x - ink.x(ink.n - 1)
                                val dy = p.y - ink.y(ink.n - 1)
                                if (dx * dx + dy * dy >= 0.64f) ink.add(p.x, p.y, pressure)
                            }
                        }
                        state.liveVersion++
                    }

                    fun finish() {
                        if (!drawing) return
                        drawing = false
                        if (activeTool == Tool.ERASER) {
                            state.commitErase(erased)
                        } else {
                            state.live?.let { if (it.n >= 1) state.commitAdd(it) }
                        }
                        state.live = null
                        state.liveVersion++
                    }

                    fun cancel() {
                        if (!drawing) return
                        drawing = false
                        if (activeTool == Tool.ERASER) state.commitErase(erased)
                        state.live = null
                        state.liveVersion++
                    }

                    while (true) {
                        val changes = event?.changes ?: listOf(down)
                        val stylus = changes.firstOrNull { it.type == PointerType.Stylus || it.type == PointerType.Eraser }
                        if (stylus != null) {
                            lastStylusMs = System.currentTimeMillis()
                            if (stylus.pressed) {
                                stylusDown = true
                                // Parmakla başlayan bir hareketin üstüne kalem gelirse, parmak hareketi iptal edilir.
                                val pressure = if (stylus.pressure > 0f) stylus.pressure.coerceIn(0.05f, 1f) else 0.5f
                                if (!drawing) begin(stylus.position, pressure, stylus.type == PointerType.Eraser)
                                else extend(stylus.position, pressure)
                            } else {
                                stylusDown = false
                                finish()
                            }
                            changes.forEach { it.consume() }
                        } else {
                            val touches = changes.filter { it.pressed }
                            val palm = stylusDown || System.currentTimeMillis() - lastStylusMs < 600
                            if (palm) {
                                // Avuç içi olabilir: yok say.
                            } else if (touches.size >= 2 || (!fingerState && touches.isNotEmpty())) {
                                if (touches.size >= 2) {
                                    sawMultiTouch = true
                                    cancel()
                                }
                                val e = event
                                if (e != null) {
                                    val zoom = e.calculateZoom()
                                    val centroid = e.calculateCentroid(useCurrent = true)
                                    val delta = e.calculatePan()
                                    if (centroid.isSpecified && (zoom != 1f || delta != Offset.Zero)) {
                                        val newScale = (scale * zoom).coerceIn(size.width / PAGE_W * 0.4f, 6f)
                                        val k = newScale / scale
                                        pan = centroid - (centroid - pan) * k + delta
                                        scale = newScale
                                    }
                                }
                            } else if (fingerState && touches.size == 1 && !sawMultiTouch) {
                                val t = touches[0]
                                if (!drawing) begin(t.position, 0.5f, false) else extend(t.position, 0.5f)
                            } else if (touches.isEmpty()) {
                                finish()
                            }
                            changes.forEach { it.consume() }
                        }
                        if (changes.none { it.pressed }) {
                            finish()
                            break
                        }
                        event = awaitPointerEvent()
                    }
                }
            },
    ) {
        val v = state.liveVersion // canlı çizimde yeniden çiz
        withTransform({
            translate(pan.x, pan.y)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            clipRect(0f, 0f, PAGE_W, PAGE_H) {
                drawPaper(paper)
                state.strokes.forEach { drawInk(it) }
                state.live?.let { drawInk(it) }
            }
        }
        if (v < 0) return@Canvas
    }
}
