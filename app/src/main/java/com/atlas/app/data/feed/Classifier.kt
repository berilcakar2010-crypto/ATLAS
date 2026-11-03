package com.atlas.app.data.feed

import java.util.Locale

/**
 * Basit, açıklanabilir sınıflandırma: kaynak tek konuluysa konusu sabittir; karma kaynaklarda
 * başlık + özetteki anahtar sözcükler sayılır. Makine öğrenmesi yoktur ve yanılabilir.
 *
 * Desen kuralı: "x*" önek eşleşmesi, boşluklu ifade metinde geçme, diğerleri tam sözcük eşleşmesi.
 */
object Classifier {
    private val patterns: Map<Category, List<String>> = mapOf(
        Category.NEURO to listOf(
            "neuron*", "neural", "brain*", "synap*", "cortex", "cortical", "hippocamp*", "neuroscience",
            "neuroscientist*", "dendrit*", "alzheimer*", "parkinson*", "cognition", "cognitive", "neurodegenerat*",
        ),
        Category.PHYSICS to listOf(
            "quantum", "particle*", "photon*", "superconduct*", "higgs", "relativity", "condensed matter",
            "laser*", "boson*", "fermion*", "physicist*", "entanglement",
        ),
        Category.COSMOLOGY to listOf(
            "galax*", "universe", "cosmic", "cosmolog*", "black hole*", "telescope*", "exoplanet*", "dark matter",
            "dark energy", "big bang", "mars", "asteroid*", "nebula*", "supernova*", "astronom*", "astrophys*",
        ),
        Category.BIOLOGY to listOf(
            "species", "genome*", "genetic*", "gene", "genes", "protein*", "cell", "cells", "dna", "rna",
            "evolution*", "bacteria*", "virus*", "vaccine*", "microbio*", "biolog*", "ecosystem*",
        ),
        Category.MATH to listOf(
            "theorem*", "conjecture*", "topology", "algebra*", "mathemati*", "proof", "geometry", "prime", "primes",
        ),
        Category.AI to listOf(
            "artificial intelligence", "machine learning", "language model*", "llm", "llms", "chatbot*",
            "deep learning", "openai", "anthropic", "deepmind", "generative", "ai",
        ),
        Category.TECH to listOf(
            "software", "chip", "chips", "semiconductor*", "robot*", "internet", "cyber*", "smartphone*",
            "startup*", "computing", "processor*", "satellite*", "encryption",
        ),
        Category.CLIMATE to listOf(
            "climate", "warming", "emission*", "carbon", "wildfire*", "glacier*", "sea level", "biodiversity",
            "ocean*", "drought*", "deforestation", "pollution", "coral", "extinction",
        ),
        Category.SUSTAINABILITY to listOf(
            "renewable*", "solar", "wind power", "clean energy", "recycl*", "sustainab*", "electric vehicle*",
            "battery", "batteries", "hydrogen", "net zero", "circular economy",
        ),
        Category.POLITICS to listOf(
            "election*", "parliament*", "sanction*", "war", "ceasefire", "diplomat*", "treaty", "government*",
            "president", "minister*", "military", "nato", "united nations", "referendum",
        ),
        Category.ECONOMICS to listOf(
            "inflation", "gdp", "economy", "economic*", "trade", "tariff*", "market*", "central bank*",
            "interest rate*", "recession", "unemployment", "fiscal", "debt",
        ),
        Category.PHILOSOPHY to listOf(
            "philosoph*", "ethic*", "metaphysic*", "epistemolog*", "consciousness", "free will", "moral*",
            "ontolog*", "existential*", "phenomenolog*",
        ),
        Category.LITERATURE to listOf(
            "novel*", "poem*", "poet*", "poetry", "author*", "fiction", "literary", "literature", "translation*",
            "memoir*", "essayist*",
        ),
        Category.SOCIOLOGY to listOf(
            "inequality", "migration", "migrant*", "society", "societies", "social*", "gender", "education",
            "poverty", "human rights", "refugee*", "demograph*", "racism", "discrimination",
        ),
    )

    fun classify(title: String, summary: String, spec: FeedSpec): Category {
        if (!spec.mixed) return spec.category
        val text = (title + " " + summary).lowercase(Locale.ROOT)
        val words = text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        var best = spec.category
        var bestScore = 0
        for ((category, list) in patterns) {
            var score = 0
            for (pattern in list) {
                score += when {
                    pattern.contains(' ') -> if (text.contains(pattern)) 2 else 0
                    pattern.endsWith("*") -> {
                        val stem = pattern.dropLast(1)
                        words.count { it.startsWith(stem) }
                    }
                    else -> words.count { it == pattern }
                }
            }
            if (score > bestScore) {
                best = category
                bestScore = score
            }
        }
        return if (bestScore >= 2) best else spec.category
    }
}
