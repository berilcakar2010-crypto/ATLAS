package com.atlas.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ATLAS yerel veritabanı (SQLite / Room).
 *
 * Bilinçli karar: fallbackToDestructiveMigration() KULLANILMIYOR. Şema değiştiğinde
 * kullanıcı verisi sessizce silinmez; her değişiklik açık bir migration gerektirir.
 */
@Database(
    entities = [
        QuestionEntity::class,
        QuestionLinkEntity::class,
        LibraryItemEntity::class,
        TagEntity::class,
        LibraryItemTagCrossRef::class,
        NotebookEntity::class,
        NotebookPageEntity::class,
        FeedSourceEntity::class,
        ArticleEntity::class,
        PaperEntity::class,
        ResearchTopicEntity::class,
        DeepDiveSessionEntity::class,
        DeepDiveMessageEntity::class,
        StrokeEntity::class,
        CategoryEntity::class,
        VocabEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AtlasDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun libraryDao(): LibraryDao
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
    abstract fun paperDao(): PaperDao
    abstract fun topicDao(): TopicDao
    abstract fun deepDiveDao(): DeepDiveDao
    abstract fun notebookDao(): NotebookDao
    abstract fun strokeDao(): StrokeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun vocabDao(): VocabDao

    companion object {
        fun create(context: Context): AtlasDatabase =
            Room.databaseBuilder(context.applicationContext, AtlasDatabase::class.java, "atlas.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}

/** v1 → v2: haber, makale, konu, Deep Dive ve çizgi tabloları eklenir. Mevcut tablolara dokunulmaz. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `feed_sources` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `category` TEXT NOT NULL, `kind` TEXT NOT NULL, `language` TEXT NOT NULL, `mixed` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `lastFetchedAt` INTEGER NOT NULL, `lastStatus` TEXT NOT NULL, `lastError` TEXT, `lastCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `articles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceId` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `title` TEXT NOT NULL, `summary` TEXT NOT NULL, `url` TEXT NOT NULL, `urlKey` TEXT NOT NULL, `publishedAt` INTEGER NOT NULL, `category` TEXT NOT NULL, `kind` TEXT NOT NULL, `language` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, `saved` INTEGER NOT NULL, `savedAt` INTEGER NOT NULL, `tags` TEXT NOT NULL, `note` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_articles_urlKey` ON `articles` (`urlKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_articles_category` ON `articles` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_articles_publishedAt` ON `articles` (`publishedAt`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `papers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `source` TEXT NOT NULL, `externalId` TEXT NOT NULL, `title` TEXT NOT NULL, `authors` TEXT NOT NULL, `abstractText` TEXT NOT NULL, `publishedAt` INTEGER NOT NULL, `url` TEXT NOT NULL, `doi` TEXT, `journal` TEXT NOT NULL, `topic` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, `saved` INTEGER NOT NULL, `savedAt` INTEGER NOT NULL, `tags` TEXT NOT NULL, `annotation` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_papers_source_externalId` ON `papers` (`source`, `externalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_papers_publishedAt` ON `papers` (`publishedAt`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `research_topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `query` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_research_topics_name` ON `research_topics` (`name`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `deep_dive_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `mode` TEXT NOT NULL, `assumptions` TEXT NOT NULL, `hypotheses` TEXT NOT NULL, `uncertainties` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `deep_dive_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `deep_dive_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_deep_dive_messages_sessionId` ON `deep_dive_messages` (`sessionId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `strokes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pageId` INTEGER NOT NULL, `tool` TEXT NOT NULL, `color` INTEGER NOT NULL, `width` REAL NOT NULL, `points` BLOB NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`pageId`) REFERENCES `notebook_pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strokes_pageId` ON `strokes` (`pageId`)")
    }
}

/** v2 → v3: kategoriler ve kayıtlı sözcükler. Mevcut tablolara dokunulmaz. */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `keywords` TEXT NOT NULL, `builtin` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `vocab_words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `word` TEXT NOT NULL, `meaning` TEXT NOT NULL, `example` TEXT NOT NULL, `note` TEXT NOT NULL, `context` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `known` INTEGER NOT NULL, `reviewCount` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vocab_words_word` ON `vocab_words` (`word`)")
    }
}
