package com.atlas.app.data.feed

import com.atlas.app.data.db.CategoryEntity
import java.util.Locale

/**
 * Basit, açıklanabilir sınıflandırma: kaynak tek konuluysa konusu sabittir; karma kaynaklarda
 * başlık + özetteki anahtar sözcükler sayılır (anahtar sözcükler Ayarlar'dan düzenlenir).
 * Makine öğrenmesi yoktur ve yanılabilir.
 *
 * Desen kuralı: "x*" önek eşleşmesi, boşluklu ifade metinde geçme (2 puan), diğerleri tam sözcük eşleşmesi.
 */
object Classifier {
    fun classify(title: String, summary: String, spec: FeedSpec, categories: List<CategoryEntity>): String {
        if (!spec.mixed) return spec.category.name
        return classifyText(title, summary, spec.category.name, categories)
    }

    fun classifyText(title: String, summary: String, fallback: String, categories: List<CategoryEntity>): String {
        val text = (title + " " + summary).lowercase(Locale.ROOT)
        val words = text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        var best = fallback
        var bestScore = 0
        for (category in categories) {
            var score = 0
            for (raw in category.keywords.split(',')) {
                val pattern = raw.trim().lowercase(Locale.ROOT)
                if (pattern.isEmpty()) continue
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
                best = category.id
                bestScore = score
            }
        }
        return if (bestScore >= 2) best else fallback
    }
}
