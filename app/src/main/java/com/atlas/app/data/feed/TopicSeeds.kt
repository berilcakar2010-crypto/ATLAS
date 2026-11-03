package com.atlas.app.data.feed

import com.atlas.app.data.db.ResearchTopicEntity

/** Başlangıç araştırma ilgi alanları (brief'ten). Radar'da eklenip çıkarılabilir. */
object TopicSeeds {
    val all: List<ResearchTopicEntity> = listOf(
        ResearchTopicEntity(name = "Hesaplamalı nörobilim", query = "computational neuroscience"),
        ResearchTopicEntity(name = "Kuramsal nörobilim", query = "theoretical neuroscience"),
        ResearchTopicEntity(name = "Sinaptik plastisite", query = "synaptic plasticity"),
        ResearchTopicEntity(name = "Kısa süreli sinaptik plastisite", query = "short-term synaptic plasticity"),
        ResearchTopicEntity(name = "Hodgkin–Huxley modelleri", query = "Hodgkin-Huxley"),
        ResearchTopicEntity(name = "Tsodyks–Markram modelleri", query = "Tsodyks-Markram"),
        ResearchTopicEntity(name = "Sinirsel kodlama", query = "neural coding"),
        ResearchTopicEntity(name = "Sinirsel dinamikler", query = "neural dynamics"),
        ResearchTopicEntity(name = "Nörodejeneratif hastalıklar", query = "neurodegenerative disease"),
        ResearchTopicEntity(name = "Hesaplamalı biyoloji", query = "computational biology"),
        ResearchTopicEntity(name = "Kuramsal fizik", query = "theoretical physics"),
        ResearchTopicEntity(name = "Kuantum mekaniği", query = "quantum mechanics"),
        ResearchTopicEntity(name = "Kozmoloji", query = "cosmology"),
        ResearchTopicEntity(name = "Karmaşık sistemler", query = "complex systems"),
        ResearchTopicEntity(name = "Matematik", query = "mathematical"),
        ResearchTopicEntity(name = "Bilgi kuramı", query = "information theory"),
    )
}
