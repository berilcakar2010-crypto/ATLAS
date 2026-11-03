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
    ],
    version = 2,
    exportSchema = true,
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

    companion object {
        fun create(context: Context): AtlasDatabase =
            Room.databaseBuilder(context.applicationContext, AtlasDatabase::class.java, "atlas.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** v1 → v2: haber, makale, konu, Deep Dive ve çizgi tabloları eklenir. Mevcut tablolara dokunulmaz. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // GENERATED-SQL-PLACEHOLDER
    }
}
