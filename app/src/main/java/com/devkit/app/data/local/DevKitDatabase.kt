package com.devkit.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ToolHistoryEntity::class,
        FavoriteToolEntity::class,
        SnippetEntity::class,
        CollectionEntity::class,
        SavedRequestEntity::class,
        RequestHistoryEntity::class,
        EnvironmentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DevKitDatabase : RoomDatabase() {
    abstract fun toolDao(): ToolDao

    companion object {
        private const val DB_NAME = "devkit.db"

        /** Example migration slot for future schema changes; see docs/DATABASE.md. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Intentionally empty until a v2 schema is defined.
            }
        }

        @Volatile
        private var instance: DevKitDatabase? = null

        fun get(context: Context): DevKitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DevKitDatabase::class.java,
                DB_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { instance = it }
        }
    }
}
