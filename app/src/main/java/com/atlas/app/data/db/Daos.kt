package com.atlas.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE status != 'ANSWERED' ORDER BY updatedAt DESC LIMIT :limit")
    fun observeOpen(limit: Int): Flow<List<QuestionEntity>>

    @Insert
    suspend fun insert(question: QuestionEntity): Long

    @Query("UPDATE questions SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, now: Long)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<LibraryItemEntity>>

    @Insert
    suspend fun insert(item: LibraryItemEntity): Long

    @Query("UPDATE library_items SET favorite = NOT favorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun delete(id: Long)
}
