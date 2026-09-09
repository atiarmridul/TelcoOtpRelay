package com.mridul.telcootprelay

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class PendingRelay(
    val id: Long,
    val webhook: String,
    val message: String,
    val sender: String,
    val smsTimestampMillis: Long,
    val createdAtMillis: Long
)

object PendingRelayStore {
    private const val DATABASE_NAME = "pending_relays.db"
    private const val DATABASE_VERSION = 1
    private const val TABLE = "pending_relays"

    fun insert(
        context: Context,
        webhook: String,
        message: String,
        sender: String,
        smsTimestampMillis: Long,
        createdAtMillis: Long = System.currentTimeMillis()
    ): Long {
        val values = ContentValues().apply {
            put("webhook", webhook)
            put("message", message)
            put("sender", sender)
            put("sms_timestamp", smsTimestampMillis)
            put("created_at", createdAtMillis)
        }
        PendingDatabase(context.applicationContext).use { database ->
            return database.writableDatabase.insertOrThrow(TABLE, null, values)
        }
    }

    fun load(context: Context, id: Long): PendingRelay? {
        PendingDatabase(context.applicationContext).use { database ->
            database.readableDatabase.query(
                TABLE,
                arrayOf("id", "webhook", "message", "sender", "sms_timestamp", "created_at"),
                "id = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) return null
                return PendingRelay(
                    id = cursor.getLong(0),
                    webhook = cursor.getString(1),
                    message = cursor.getString(2),
                    sender = cursor.getString(3),
                    smsTimestampMillis = cursor.getLong(4),
                    createdAtMillis = cursor.getLong(5)
                )
            }
        }
    }

    fun delete(context: Context, id: Long): Boolean {
        PendingDatabase(context.applicationContext).use { database ->
            return database.writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString())) > 0
        }
    }

    private class PendingDatabase(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE $TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    webhook TEXT NOT NULL,
                    message TEXT NOT NULL,
                    sender TEXT NOT NULL,
                    sms_timestamp INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(
            database: SQLiteDatabase,
            oldVersion: Int,
            newVersion: Int
        ) = Unit
    }
}
