package com.example.clipy.clipy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.clipy.clipy.model.ExportRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipyDao {
  @Query("SELECT * FROM export_records ORDER BY createdAt DESC")
  fun observeHistory(): Flow<List<ExportRecord>>

  @Query("SELECT * FROM export_records WHERE id = :recordId LIMIT 1")
  suspend fun getRecordById(recordId: Long): ExportRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecord(record: ExportRecord)
}
