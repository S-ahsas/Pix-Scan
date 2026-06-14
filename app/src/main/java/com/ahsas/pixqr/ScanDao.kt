package com.ahsas.pixqr

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(record: ScanRecord)

    @Delete
    suspend fun delete(record: ScanRecord)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_history WHERE rawContent LIKE '%' || :query || '%' OR cleanedContent LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScans(query: String): Flow<List<ScanRecord>>

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()

     @Query("SELECT * FROM scan_history WHERE rawContent = :content LIMIT 1")
     suspend fun findByContent(content: String): ScanRecord?
     @Update
     suspend fun update(record: ScanRecord)
}
