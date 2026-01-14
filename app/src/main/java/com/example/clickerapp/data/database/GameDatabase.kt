package com.example.clickerapp.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GameStateEntity::class, AchievementEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        private const val TAG = "GameDatabase"
        
        fun create(context: Context): GameDatabase {
            Log.d(TAG, "Creating database: game_database.db")
            return try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                Log.d(TAG, "Database created successfully")
                db
            } catch (e: Exception) {
                Log.e(TAG, "Error creating database", e)
                throw e
            }
        }

        private val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE game_state ADD COLUMN lastSeenEpochMs INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS achievements (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "unlockedAtEpochMs INTEGER NOT NULL" +
                    ")"
            )
        }

        private val MIGRATION_2_3 = androidx.room.migration.Migration(2, 3) { db ->
            db.execSQL("ALTER TABLE game_state ADD COLUMN pointsMultiplier INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE game_state ADD COLUMN autoClickerSpeed INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE game_state ADD COLUMN comboBonus INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN offlineMultiplier INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE game_state ADD COLUMN premiumUpgrade1 INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN premiumUpgrade2 INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN lastTapTime INTEGER NOT NULL DEFAULT 0")
        }

        private val MIGRATION_3_4 = androidx.room.migration.Migration(3, 4) { db ->
            db.execSQL("ALTER TABLE game_state ADD COLUMN goatPenLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN goatFoodLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN fridgeLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN printerLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN scannerLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN printer3dLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN cryptoAmount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN miningPower INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN lastMiningTime INTEGER NOT NULL DEFAULT 0")
        }

        private val MIGRATION_4_5 = androidx.room.migration.Migration(4, 5) { db ->
            db.execSQL("ALTER TABLE game_state ADD COLUMN hasSoldCrypto INTEGER NOT NULL DEFAULT 0")
        }

        private val MIGRATION_5_6 = androidx.room.migration.Migration(5, 6) { db ->
            // Престиж система
            db.execSQL("ALTER TABLE game_state ADD COLUMN prestigeLevel INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN prestigePoints INTEGER NOT NULL DEFAULT 0")
            // Временные бусты
            db.execSQL("ALTER TABLE game_state ADD COLUMN boostMultiplier INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE game_state ADD COLUMN boostEndTime INTEGER NOT NULL DEFAULT 0")
            // Квесты
            db.execSQL("ALTER TABLE game_state ADD COLUMN activeQuestType TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE game_state ADD COLUMN activeQuestProgress INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN activeQuestTarget INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN activeQuestReward INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE game_state ADD COLUMN lastQuestResetTime INTEGER NOT NULL DEFAULT 0")
            // События
            db.execSQL("ALTER TABLE game_state ADD COLUMN activeEventType TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE game_state ADD COLUMN activeEventEndTime INTEGER NOT NULL DEFAULT 0")
        }
    }
}

