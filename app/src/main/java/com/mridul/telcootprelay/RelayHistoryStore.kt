package com.mridul.telcootprelay

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class RelayHistoryEntry(
    val id: Long,
    val status: LastRelayStatus
)

object RelayHistoryStore {
    private const val DATABASE_NAME = "relay_history.db"
    private const val DATABASE_VERSION = 1
    private const val TABLE = "relay_history"
    private const val MIGRATED_KEY = "relay_history_migrated"

    fun migrateLatestIfNeeded(context: Context, preferences: SharedPreferences) {
        if (preferences.getBoolean(MIGRATED_KEY, false)) return

        LastRelayStore.load(preferences)?.let { append(context, it) }
        preferences.edit().putBoolean(MIGRATED_KEY, true).commit()
    }

    fun append(context: Context, status: LastRelayStatus) {
        val values = ContentValues().apply {
            put("result", status.result.name)
            put("sender", status.sender)
            put("timestamp", status.timestampMillis)
            put("failure_kind", status.failureKind?.name)
            put("http_status", status.httpStatusCode)
        }
        HistoryDatabase(context.applicationContext).use { database ->
            database.writableDatabase.insertOrThrow(TABLE, null, values)
        }
    }

    fun loadAll(context: Context): List<RelayHistoryEntry> {
        val entries = mutableListOf<RelayHistoryEntry>()
        HistoryDatabase(context.applicationContext).use { database ->
            database.readableDatabase.query(
                TABLE,
                arrayOf("id", "result", "sender", "timestamp", "failure_kind", "http_status"),
                null,
                null,
                null,
                null,
                "id DESC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val status = LastRelayStatus.fromStored(
                        result = cursor.getString(1),
                        sender = cursor.getString(2),
                        timestampMillis = cursor.getLong(3),
                        failureKind = cursor.getString(4),
                        httpStatusCode = if (cursor.isNull(5)) -1 else cursor.getInt(5)
                    ) ?: continue
                    entries += RelayHistoryEntry(cursor.getLong(0), status)
                }
            }
        }
        return entries
    }

    fun clear(context: Context) {
        HistoryDatabase(context.applicationContext).use { database ->
            database.writableDatabase.delete(TABLE, null, null)
        }
    }

    private class HistoryDatabase(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE $TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    result TEXT NOT NULL,
                    sender TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    failure_kind TEXT,
                    http_status INTEGER
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
