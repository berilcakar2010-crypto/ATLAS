package com.atlas.app.sync

import com.atlas.app.data.db.ArticleEntity
import com.atlas.app.data.db.AtlasDatabase
import com.atlas.app.data.db.FeedSourceEntity
import com.atlas.app.data.db.PaperEntity
import com.atlas.app.data.feed.Category
import com.atlas.app.data.feed.Classifier
import com.atlas.app.data.feed.FeedCatalog
import com.atlas.app.data.feed.FeedSpec
import com.atlas.app.data.feed.TopicSeeds
import com.atlas.app.network.ArxivClient
import com.atlas.app.network.FeedParser
import com.atlas.app.network.Http
import com.atlas.app.network.PaperDraft
import com.atlas.app.network.PubMedClient
import com.atlas.app.network.TextUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class SyncStatus(
    val running: Boolean = false,
    val phase: String = "",
    val done: Int = 0,
    val total: Int = 0,
    val lastFinishedAt: Long = 0,
    val lastNewArticles: Int = 0,
    val lastNewPapers: Int = 0,
    val lastFailedFeeds: Int = 0,
)

/**
 * Haber ve makale senkronizasyonu. Yalnızca açık, genel RSS/Atom adreslerine ve arXiv/PubMed
 * API'lerine GET isteği atar. Kullanıcı verisi hiçbir yere gönderilmez.
 */
class SyncManager(private val db: AtlasDatabase) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Katalogdaki yeni kaynakları ve ilk ilgi alanlarını ekler; kullanıcı ayarlarına dokunmaz. */
    suspend fun seed() {
        val existing = db.feedDao().getIds().toSet()
        val fresh = FeedCatalog.all.filter { it.id !in existing }.map { it.toEntity() }
        if (fresh.isNotEmpty()) db.feedDao().insertAll(fresh)
        // Kataloğu güncelle (adres değişmiş olabilir), "enabled" korunur.
        FeedCatalog.all.filter { it.id in existing }.forEach {
            db.feedDao().updateMeta(it.id, it.name, it.url, it.category.name, it.kind.name, it.language, it.mixed)
        }
        if (db.topicDao().count() == 0) db.topicDao().insertAll(TopicSeeds.all)
    }

    suspend fun syncAll(includePapers: Boolean, forcePapers: Boolean) {
        if (!mutex.tryLock()) return
        try {
            seed()
            var newArticles = 0
            var newPapers = 0
            var failed = 0
            val feeds = db.feedDao().getEnabled()
            val topics = if (includePapers) db.topicDao().getEnabled() else emptyList()
            val total = feeds.size + topics.size * 2
            var done = 0
            _status.value = SyncStatus(running = true, phase = "Haber kaynakları", total = total)

            val gate = Semaphore(4)
            val results = coroutineScope {
                feeds.map { feed ->
                    async {
                        gate.withPermit {
                            val r = syncFeed(feed)
                            synchronized(this@SyncManager) {
                                done++
                                _status.value = _status.value.copy(done = done)
                            }
                            r
                        }
                    }
                }.awaitAll()
            }
            results.forEach { (n, ok) ->
                newArticles += n
                if (!ok) failed++
            }

            if (includePapers) {
                val last = lastPaperSync
                val due = forcePapers || System.currentTimeMillis() - last > 12L * 3600_000
                if (due) {
                    _status.value = _status.value.copy(phase = "arXiv ve PubMed")
                    for (topic in topics) {
                        newPapers += syncTopic(topic.name, topic.query, "ARXIV") { ArxivClient.search(it, 15) }
                        done++
                        _status.value = _status.value.copy(done = done)
                        delay(3300) // arXiv API yönergesi: istekler arası en az ~3 sn
                        newPapers += syncTopic(topic.name, topic.query, "PUBMED") { PubMedClient.search(it, 15) }
                        done++
                        _status.value = _status.value.copy(done = done)
                        delay(450)
                    }
                    lastPaperSync = System.currentTimeMillis()
                }
            }

            val now = System.currentTimeMillis()
            db.articleDao().deleteOldUnsaved(now - 45L * 24 * 3600_000)
            db.paperDao().deleteOldUnsaved(now - 120L * 24 * 3600_000)
            _status.value = SyncStatus(
                running = false,
                lastFinishedAt = now,
                lastNewArticles = newArticles,
                lastNewPapers = newPapers,
                lastFailedFeeds = failed,
                total = total,
                done = total,
            )
        } catch (e: CancellationException) {
            _status.value = _status.value.copy(running = false)
            throw e
        } catch (e: Exception) {
            _status.value = _status.value.copy(running = false, phase = "Hata: ${e.message}")
        } finally {
            mutex.unlock()
        }
    }

    @Volatile
    private var lastPaperSync: Long = 0

    private suspend fun syncFeed(feed: FeedSourceEntity): Pair<Int, Boolean> {
        val spec = FeedSpec(
            id = feed.id, name = feed.name, url = feed.url,
            category = Category.fromName(feed.category), kind = com.atlas.app.data.feed.SourceKind.fromName(feed.kind),
            language = feed.language, mixed = feed.mixed,
        )
        return try {
            val xml = Http.get(feed.url, mapOf("Accept" to "application/rss+xml, application/atom+xml, application/xml, text/xml, */*"))
            val items = FeedParser.parse(xml)
            val now = System.currentTimeMillis()
            val entities = items.map {
                ArticleEntity(
                    sourceId = feed.id,
                    sourceName = feed.name,
                    title = it.title,
                    summary = it.summary,
                    url = it.link,
                    urlKey = TextUtil.urlKey(it.link),
                    publishedAt = (it.publishedAt ?: now).coerceAtMost(now),
                    category = Classifier.classify(it.title, it.summary, spec).name,
                    kind = feed.kind,
                    language = feed.language,
                    fetchedAt = now,
                )
            }
            val ids = db.articleDao().insertAll(entities)
            val added = ids.count { it != -1L }
            db.feedDao().updateStatus(feed.id, "OK", null, now, items.size)
            added to true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            db.feedDao().updateStatus(feed.id, "FAILED", (e.message ?: e.javaClass.simpleName).take(160), System.currentTimeMillis(), 0)
            0 to false
        }
    }

    private suspend fun syncTopic(
        topic: String,
        query: String,
        source: String,
        fetch: suspend (String) -> List<PaperDraft>,
    ): Int = try {
        val now = System.currentTimeMillis()
        val drafts = fetch(query)
        val ids = db.paperDao().insertAll(drafts.map { it.toEntity(topic, now) })
        ids.count { it != -1L }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        0
    }

    /** Radar'daki tek konuyu hemen yeniler. */
    suspend fun refreshTopic(topicName: String, query: String): Int {
        var n = syncTopic(topicName, query, "ARXIV") { ArxivClient.search(it, 20) }
        delay(3300)
        n += syncTopic(topicName, query, "PUBMED") { PubMedClient.search(it, 20) }
        return n
    }
}

private fun FeedSpec.toEntity() = FeedSourceEntity(
    id = id, name = name, url = url, category = category.name, kind = kind.name, language = language,
    mixed = mixed, enabled = true, lastFetchedAt = 0, lastStatus = "PENDING", lastError = null, lastCount = 0,
)

private fun PaperDraft.toEntity(topic: String, now: Long) = PaperEntity(
    source = source, externalId = externalId, title = title, authors = authors, abstractText = abstractText,
    publishedAt = publishedAt.coerceAtMost(now), url = url, doi = doi, journal = journal, topic = topic, fetchedAt = now,
)
