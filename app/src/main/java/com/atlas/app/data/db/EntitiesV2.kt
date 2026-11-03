package com.atlas.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Bir haber kaynağı ve son senkronizasyon durumu. */
@Entity(tableName = "feed_sources")
data class FeedSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val category: String,
    val kind: String,
    val language: String,
    val mixed: Boolean,
    val enabled: Boolean,
    val lastFetchedAt: Long,
    /** PENDING, OK veya FAILED. */
    val lastStatus: String,
    val lastError: String?,
    val lastCount: Int,
)

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["urlKey"], unique = true),
        Index(value = ["category"]),
        Index(value = ["publishedAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    /** Kaynağın kendi özeti (ATLAS tarafından yazılmaz). */
    val summary: String,
    val url: String,
    /** Normalleştirilmiş adresin özeti; çift kaydı önler. */
    val urlKey: String,
    val publishedAt: Long,
    val category: String,
    val kind: String,
    val language: String,
    val fetchedAt: Long,
    val saved: Boolean = false,
    val savedAt: Long = 0,
    /** Virgülle ayrılmış etiketler. */
    val tags: String = "",
    /** Kullanıcının kendi yorumu; kaynaktan ayrı tutulur. */
    val note: String = "",
)

@Entity(
    tableName = "papers",
    indices = [
        Index(value = ["source", "externalId"], unique = true),
        Index(value = ["publishedAt"]),
    ],
)
data class PaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ARXIV veya PUBMED. */
    val source: String,
    val externalId: String,
    val title: String,
    val authors: String,
    val abstractText: String,
    val publishedAt: Long,
    val url: String,
    val doi: String?,
    val journal: String,
    val topic: String,
    val fetchedAt: Long,
    val saved: Boolean = false,
    val savedAt: Long = 0,
    val tags: String = "",
    val annotation: String = "",
)

@Entity(tableName = "research_topics", indices = [Index(value = ["name"], unique = true)])
data class ResearchTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "deep_dive_sessions")
data class DeepDiveSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** OFFLINE veya AI. */
    val mode: String,
    val assumptions: String = "",
    val hypotheses: String = "",
    val uncertainties: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "deep_dive_messages",
    foreignKeys = [
        ForeignKey(
            entity = DeepDiveSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class DeepDiveMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    /** USER, AI veya SYSTEM. */
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Defter sayfasındaki tek bir çizgi. Noktalar [x, y, basınç] üçlüleri olarak ikili paketlenir. */
@Entity(
    tableName = "strokes",
    foreignKeys = [
        ForeignKey(
            entity = NotebookPageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["pageId"])],
)
data class StrokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    val tool: String,
    val color: Int,
    val width: Float,
    val points: ByteArray,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Radar'da konu başına makale sayısı (sorgu sonucu). */
data class TopicCount(
    val topic: String,
    val count: Int,
)
