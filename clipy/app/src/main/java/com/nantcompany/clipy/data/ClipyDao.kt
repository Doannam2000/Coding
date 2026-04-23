package com.nantcompany.clipy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nantcompany.clipy.model.ExportRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipyDao {
  @Query("SELECT * FROM export_records ORDER BY createdAt DESC")
  fun observeHistory(): Flow<List<ExportRecord>>

  @Query("SELECT * FROM export_records WHERE id = :recordId LIMIT 1")
  suspend fun getRecordById(recordId: Long): ExportRecord?

  @Query("DELETE FROM export_records")
  suspend fun clearHistory()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecord(record: ExportRecord)
}
