package com.atlas.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_sources ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FeedSourceEntity>>

    @Query("SELECT * FROM feed_sources WHERE enabled = 1")
    suspend fun getEnabled(): List<FeedSourceEntity>

    @Query("SELECT * FROM feed_sources")
    suspend fun observeAllOnce(): List<FeedSourceEntity>

    @Query("SELECT id FROM feed_sources")
    suspend fun getIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(feeds: List<FeedSourceEntity>)

    /** Katalog yenilemesi kullanıcının seçtiği kategoriye dokunmaz. */
    @Query("UPDATE feed_sources SET name = :name, url = :url, kind = :kind, language = :language, mixed = :mixed WHERE id = :id")
    suspend fun updateMeta(id: String, name: String, url: String, kind: String, language: String, mixed: Boolean)

    @Query("UPDATE feed_sources SET category = :category WHERE id = :id")
    suspend fun setCategory(id: String, category: String)

    /** Yalnızca kullanıcının eklediği ("custom-") kaynaklar silinebilir. */
    @Query("DELETE FROM feed_sources WHERE id = :id AND id LIKE 'custom-%'")
    suspend fun deleteCustom(id: String)

    @Query("UPDATE feed_sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query(
        "UPDATE feed_sources SET lastStatus = :status, lastError = :error, lastFetchedAt = :at, " +
            "lastCount = :count WHERE id = :id",
    )
    suspend fun updateStatus(id: String, status: String, error: String?, at: Long, count: Int)
}

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>): List<Long>

    @Query(
        "SELECT * FROM articles WHERE (:category = '' OR category = :category) " +
            "AND publishedAt >= :since " +
            "AND (:query = '' OR title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' " +
            "OR tags LIKE '%' || :query || '%' OR sourceName LIKE '%' || :query || '%') " +
            "ORDER BY publishedAt DESC LIMIT :limit",
    )
    fun observeFiltered(category: String, query: String, since: Long, limit: Int): Flow<List<ArticleEntity>>

    @Query(
        "SELECT * FROM articles WHERE category IN (:categories) AND publishedAt >= :since " +
            "ORDER BY publishedAt DESC LIMIT :limit",
    )
    fun observeByCategories(categories: List<String>, since: Long, limit: Int): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE publishedAt >= :since ORDER BY publishedAt DESC LIMIT :limit")
    fun observeRecent(since: Long, limit: Int): Flow<List<ArticleEntity>>

    @Query("SELECT category AS topic, COUNT(*) AS count FROM articles WHERE publishedAt >= :since GROUP BY category")
    fun observeCategoryCounts(since: Long): Flow<List<TopicCount>>

    @Query("SELECT COUNT(DISTINCT sourceId) FROM articles WHERE publishedAt >= :since")
    fun observeActiveSourceCount(since: Long): Flow<Int>

    @Query("SELECT * FROM articles WHERE saved = 1 ORDER BY savedAt DESC")
    fun observeSaved(): Flow<List<ArticleEntity>>

    @Query("SELECT COUNT(*) FROM articles")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE saved = 1")
    fun observeSavedCount(): Flow<Int>

    @Query("UPDATE articles SET saved = :saved, savedAt = :now WHERE id = :id")
    suspend fun setSaved(id: Long, saved: Boolean, now: Long)

    @Query("SELECT * FROM articles WHERE sourceId IN (:sourceIds)")
    suspend fun getBySources(sourceIds: List<String>): List<ArticleEntity>

    @Query("UPDATE articles SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: String)

    @Query("UPDATE articles SET tags = :tags WHERE id = :id")
    suspend fun setTags(id: Long, tags: String)

    @Query("UPDATE articles SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String)

    /** Kaydedilmemiş eski haberleri temizler; kaydedilenlere dokunmaz. */
    @Query("DELETE FROM articles WHERE saved = 0 AND fetchedAt < :before")
    suspend fun deleteOldUnsaved(before: Long)
}

@Dao
interface PaperDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(papers: List<PaperEntity>): List<Long>

    @Query(
        "SELECT * FROM papers WHERE (:topic = '' OR topic = :topic) AND publishedAt >= :since " +
            "AND (:query = '' OR title LIKE '%' || :query || '%' OR abstractText LIKE '%' || :query || '%' " +
            "OR authors LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') " +
            "ORDER BY publishedAt DESC LIMIT :limit",
    )
    fun observeFiltered(topic: String, query: String, since: Long, limit: Int): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE saved = 1 ORDER BY savedAt DESC")
    fun observeSaved(): Flow<List<PaperEntity>>

    @Query("SELECT topic AS topic, COUNT(*) AS count FROM papers GROUP BY topic ORDER BY count DESC")
    fun observeTopicCounts(): Flow<List<TopicCount>>

    @Query("SELECT COUNT(*) FROM papers")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM papers WHERE saved = 1")
    fun observeSavedCount(): Flow<Int>

    @Query("UPDATE papers SET saved = :saved, savedAt = :now WHERE id = :id")
    suspend fun setSaved(id: Long, saved: Boolean, now: Long)

    @Query("UPDATE papers SET saved = 1, savedAt = :now WHERE source = :source AND externalId = :externalId")
    suspend fun markSaved(source: String, externalId: String, now: Long)

    @Query("UPDATE papers SET annotation = :annotation WHERE id = :id")
    suspend fun setAnnotation(id: Long, annotation: String)

    @Query("UPDATE papers SET tags = :tags WHERE id = :id")
    suspend fun setTags(id: Long, tags: String)

    @Query("DELETE FROM papers WHERE saved = 0 AND fetchedAt < :before")
    suspend fun deleteOldUnsaved(before: Long)
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM research_topics ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<ResearchTopicEntity>>

    @Query("SELECT * FROM research_topics WHERE enabled = 1 ORDER BY createdAt ASC, id ASC")
    suspend fun getEnabled(): List<ResearchTopicEntity>

    @Query("SELECT COUNT(*) FROM research_topics")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(topics: List<ResearchTopicEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(topic: ResearchTopicEntity): Long

    @Query("UPDATE research_topics SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM research_topics WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DeepDiveDao {
    @Query("SELECT * FROM deep_dive_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<DeepDiveSessionEntity>>

    @Query("SELECT * FROM deep_dive_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<DeepDiveSessionEntity?>

    @Query("SELECT * FROM deep_dive_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    fun observeMessages(sessionId: Long): Flow<List<DeepDiveMessageEntity>>

    @Query("SELECT * FROM deep_dive_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    suspend fun observeMessagesOnce(sessionId: Long): List<DeepDiveMessageEntity>

    @Insert
    suspend fun insertSession(session: DeepDiveSessionEntity): Long

    @Update
    suspend fun updateSession(session: DeepDiveSessionEntity)

    @Insert
    suspend fun insertMessage(message: DeepDiveMessageEntity): Long

    @Query("DELETE FROM deep_dive_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY createdAt DESC")
    fun observeNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebook_pages WHERE notebookId = :notebookId ORDER BY pageIndex ASC")
    fun observePages(notebookId: Long): Flow<List<NotebookPageEntity>>

    @Query("SELECT * FROM notebook_pages WHERE id = :id")
    suspend fun getPage(id: Long): NotebookPageEntity?

    @Query("SELECT * FROM notebook_pages WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT 50")
    fun searchPages(query: String): Flow<List<NotebookPageEntity>>

    @Insert
    suspend fun insertNotebook(notebook: NotebookEntity): Long

    @Query("UPDATE notebooks SET title = :title WHERE id = :id")
    suspend fun renameNotebook(id: Long, title: String)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteNotebook(id: Long)

    @Insert
    suspend fun insertPage(page: NotebookPageEntity): Long

    @Query("UPDATE notebook_pages SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun setPageTitle(id: Long, title: String, now: Long)

    @Query("UPDATE notebook_pages SET paper = :paper, updatedAt = :now WHERE id = :id")
    suspend fun setPagePaper(id: Long, paper: String, now: Long)

    @Query("UPDATE notebook_pages SET updatedAt = :now WHERE id = :id")
    suspend fun touchPage(id: Long, now: Long)

    @Query("DELETE FROM notebook_pages WHERE id = :id")
    suspend fun deletePage(id: Long)

    @Query("SELECT COALESCE(MAX(pageIndex), -1) FROM notebook_pages WHERE notebookId = :notebookId")
    suspend fun maxPageIndex(notebookId: Long): Int

    @Query("SELECT COUNT(*) FROM notebook_pages")
    fun observePageCount(): Flow<Int>

    @Query("SELECT * FROM notebook_pages WHERE notebookId = :notebookId ORDER BY pageIndex ASC")
    suspend fun getPages(notebookId: Long): List<NotebookPageEntity>
}

@Dao
interface StrokeDao {
    @Query("SELECT * FROM strokes WHERE pageId = :pageId ORDER BY id ASC")
    suspend fun getStrokes(pageId: Long): List<StrokeEntity>

    @Insert
    suspend fun insert(stroke: StrokeEntity): Long

    @Query("DELETE FROM strokes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM strokes WHERE pageId = :pageId")
    suspend fun clearPage(pageId: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name COLLATE NOCASE")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND builtin = 0")
    suspend fun deleteCustom(id: String)
}

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab_words ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VocabEntity>>

    @Query("SELECT COUNT(*) FROM vocab_words")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocab_words WHERE lower(word) = lower(:word)")
    suspend fun countWord(word: String): Int

    @Insert
    suspend fun insert(item: VocabEntity): Long

    @Update
    suspend fun update(item: VocabEntity)

    @Query("DELETE FROM vocab_words WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE vocab_words SET known = :known, reviewCount = reviewCount + 1 WHERE id = :id")
    suspend fun markReviewed(id: Long, known: Boolean)
}
