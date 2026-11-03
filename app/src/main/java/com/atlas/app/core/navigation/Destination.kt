package com.atlas.app.core.navigation

/** Ana modüller; sidebar sırası bu sıradır. */
enum class Destination(val route: String, val index: String, val title: String) {
    CommandCenter("command", "01", "Command Center"),
    DailyIntelligence("intelligence", "02", "Daily Intelligence"),
    ResearchRadar("radar", "03", "Research Radar"),
    DeepDive("deepdive", "04", "Deep Dive"),
    Library("library", "05", "Intellectual Library"),
    Questions("questions", "06", "Question Engine"),
    Notebook("notebook", "07", "Research Notebook"),
    Sustainability("sustainability", "08", "Sustainability Monitor"),
    OfflineArchive("archive", "09", "Offline Archive"),
    Dictionary("dictionary", "10", "Sözlük · Tureng"),
    Settings("settings", "11", "Settings");

    val sectionLabel: String get() = "SEC. $index — $title"

    companion object {
        fun fromRoute(route: String?): Destination? = entries.firstOrNull { it.route == route }
    }
}
