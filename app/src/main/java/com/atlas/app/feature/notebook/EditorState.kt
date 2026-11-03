package com.atlas.app.feature.notebook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.atlas.app.data.db.AtlasDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Açık sayfanın bellekteki hali. Veritabanı yazmaları tek bir kuyrukta sırayla yapılır;
 * böylece hızlı geri al/yinele sırasında kayıt sırası bozulmaz.
 */
class EditorState(private val db: AtlasDatabase, scope: CoroutineScope) {
    val strokes = mutableStateListOf<Ink>()
    var pageId by mutableLongStateOf(-1L)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** Canlı çizim sırasında yeniden çizimi tetikler. */
    var liveVersion by mutableIntStateOf(0)
    var live: Ink? = null

    private sealed interface Action {
        class Add(val ink: Ink) : Action
        class Remove(val inks: List<Ink>) : Action
    }

    private val undoStack = ArrayDeque<Action>()
    private val redoStack = ArrayDeque<Action>()
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val loadScope = scope

    init {
        scope.launch { for (op in queue) runCatching { op() } }
    }

    private fun flags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private fun enqueue(block: suspend () -> Unit) {
        queue.trySend(block)
    }

    private fun persistInsert(page: Long, ink: Ink) = enqueue {
        ink.id = db.strokeDao().insert(ink.toEntity(page))
        db.notebookDao().touchPage(page, System.currentTimeMillis())
    }

    private fun persistDelete(page: Long, ink: Ink) = enqueue {
        if (ink.id != 0L) db.strokeDao().delete(ink.id)
        db.notebookDao().touchPage(page, System.currentTimeMillis())
    }

    fun load(id: Long) {
        if (id == pageId) return
        pageId = id
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
        live = null
        flags()
        loadScope.launch {
            val list = db.strokeDao().getStrokes(id).map { it.toInk() }
            if (pageId == id) strokes.addAll(list)
        }
    }

    fun commitAdd(ink: Ink) {
        strokes.add(ink)
        undoStack.addLast(Action.Add(ink))
        redoStack.clear()
        persistInsert(pageId, ink)
        flags()
    }

    /** Silgi hareketi sırasında çağrılır; silinenleri hemen listeden çıkarır ve döndürür. */
    fun eraseAt(x: Float, y: Float, radius: Float): List<Ink> {
        val hit = strokes.filter { it.hitBy(x, y, radius) }
        if (hit.isNotEmpty()) strokes.removeAll(hit.toSet())
        return hit
    }

    fun commitErase(removed: List<Ink>) {
        if (removed.isEmpty()) return
        undoStack.addLast(Action.Remove(removed))
        redoStack.clear()
        val page = pageId
        removed.forEach { persistDelete(page, it) }
        flags()
    }

    fun clearPage() {
        val all = strokes.toList()
        strokes.clear()
        commitErase(all)
    }

    fun undo() {
        val a = undoStack.removeLastOrNull() ?: return
        val page = pageId
        when (a) {
            is Action.Add -> {
                strokes.remove(a.ink)
                persistDelete(page, a.ink)
            }
            is Action.Remove -> {
                strokes.addAll(a.inks)
                a.inks.forEach { persistInsert(page, it) }
            }
        }
        redoStack.addLast(a)
        flags()
    }

    fun redo() {
        val a = redoStack.removeLastOrNull() ?: return
        val page = pageId
        when (a) {
            is Action.Add -> {
                strokes.add(a.ink)
                persistInsert(page, a.ink)
            }
            is Action.Remove -> {
                strokes.removeAll(a.inks.toSet())
                a.inks.forEach { persistDelete(page, it) }
            }
        }
        undoStack.addLast(a)
        flags()
    }
}
