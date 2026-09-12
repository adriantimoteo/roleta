package com.roleta.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.roleta.app.data.db.dao.ItemDao
import com.roleta.app.data.db.dao.ListDao
import com.roleta.app.data.db.dao.PickHistoryDao
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.data.db.entity.ListEntity
import com.roleta.app.data.db.entity.PickHistoryEntity

@Database(
    entities = [ListEntity::class, ItemEntity::class, PickHistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class RoletaDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao
    abstract fun pickHistoryDao(): PickHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN skip_count INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
