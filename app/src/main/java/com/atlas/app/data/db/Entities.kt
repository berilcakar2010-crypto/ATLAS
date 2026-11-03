package com.atlas.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Soru Motoru durumları. Veritabanında enum adı (String) olarak saklanır. */
enum class QuestionStatus(val label: String) {
    UNEXPLORED("Keşfedilmedi"),
    EXPLORING("İnceleniyor"),
    NEEDS_EVIDENCE("Kanıt gerekli"),
    PARTIAL("Kısmen yanıtlı"),
    ANSWERED("Yanıtlandı"),
    REOPENED("Yeniden açıldı");

    val isOpen: Boolean get() = this != ANSWERED

    companion object {
        fun fromName(name: String): QuestionStatus =
            entries.firstOrNull { it.name == name } ?: UNEXPLORED
    }
}

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val status: String = QuestionStatus.UNEXPLORED.name,
    val hypothesis: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Sorular arası bağlantı (ileride bilgi grafiğinin kenarları). */
@Entity(
    tableName = "question_links",
    primaryKeys = ["fromId", "toId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("toId")],
)
data class QuestionLinkEntity(
    val fromId: Long,
    val toId: Long,
)

/** Kütüphane kaydı. Phase 3'te yalnızca type = "NOTE" kullanılıyor. */
@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = "NOTE",
    val title: String,
    val body: String = "",
    val url: String? = null,
    val source: String? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "library_item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class LibraryItemTagCrossRef(
    val itemId: Long,
    val tagId: Long,
)

/** Defter üst verisi. Çizgi (stroke) verisi Phase 5'te eklenecek. */
@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "notebook_pages",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("notebookId")],
)
data class NotebookPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notebookId: Long,
    val pageIndex: Int,
    val paper: String = "DOT",
    val title: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
