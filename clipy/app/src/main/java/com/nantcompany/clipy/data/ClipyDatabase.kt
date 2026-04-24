package com.nantcompany.clipy.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nantcompany.clipy.model.ExportRecord

@Database(entities = [ExportRecord::class], version = 1, exportSchema = false)
abstract class ClipyDatabase : RoomDatabase() {
  abstract fun clipyDao(): ClipyDao
}
