package com.atlas.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    ],
    version = 1,
    exportSchema = false,
)
abstract class AtlasDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        fun create(context: Context): AtlasDatabase =
            Room.databaseBuilder(context.applicationContext, AtlasDatabase::class.java, "atlas.db")
                .build()
    }
}
