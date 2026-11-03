package com.atlas.app.feature.command

import android.content.Context

enum class ModuleId(val label: String, val defaultSpan: Int) {
    BRIEFING("Günlük bülten", 2),
    TOPICS("Araştırma konuları", 1),
    SYNC("Eşitleme durumu", 1),
    RECENT("Son kaydedilenler", 1),
    QUESTIONS("Çözülmemiş sorular", 1),
    NOTEBOOK("Defter", 1),
}

data class Slot(val id: ModuleId, val span: Int, val hidden: Boolean)

/** Command Center düzenini (sıra, genişlik 1–3, gizleme) bu cihazda saklar. */
class LayoutStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("atlas_layout", Context.MODE_PRIVATE)

    fun load(): List<Slot> {
        val saved = prefs.getString("slots", null)
            ?.split(';')
            ?.mapNotNull { part ->
                val f = part.split(',')
                val id = ModuleId.entries.firstOrNull { it.name == f.getOrNull(0) } ?: return@mapNotNull null
                Slot(id, (f.getOrNull(1)?.toIntOrNull() ?: id.defaultSpan).coerceIn(1, 3), f.getOrNull(2) == "1")
            }
            .orEmpty()
        val missing = ModuleId.entries.filter { m -> saved.none { it.id == m } }.map { Slot(it, it.defaultSpan, false) }
        return saved + missing
    }

    fun save(slots: List<Slot>) {
        prefs.edit().putString("slots", slots.joinToString(";") { "${it.id.name},${it.span},${if (it.hidden) 1 else 0}" }).apply()
    }

    fun reset() = prefs.edit().remove("slots").apply()
}
