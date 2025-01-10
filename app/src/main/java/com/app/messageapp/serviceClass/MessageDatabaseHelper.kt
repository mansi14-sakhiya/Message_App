package com.app.messageapp.serviceClass

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.app.messageapp.messageView.Message

class MessageDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_SENDER TEXT, $COLUMN_MESSAGE TEXT, $COLUMN_TIMESTAMP LONG, $COLUMN_IS_SENT INTEGER)"
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertMessage(sender: String, message: String, timestamp: Long, isSent: Boolean) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_SENDER, sender)
            put(COLUMN_MESSAGE, message)
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_IS_SENT, if (isSent) 1 else 0)
        }
        db.insert(TABLE_NAME, null, contentValues)
        db.close()
    }

    fun getMessagesForRecipient(recipient: String): List<Message> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_SENDER, COLUMN_MESSAGE, COLUMN_TIMESTAMP, COLUMN_IS_SENT),
            "$COLUMN_SENDER = ?",
            arrayOf(recipient),
            null, null, "$COLUMN_TIMESTAMP ASC"
        )

        val messages = mutableListOf<Message>()
        while (cursor.moveToNext()) {
//            val sender = cursor.getString(cursor.getColumnIndex(COLUMN_SENDER))
            val message = cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE))
            val timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
            val isSent = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_SENT)) == 1
            messages.add(Message(message, isSent, timestamp))
        }
        cursor.close()
        db.close()
        return messages
    }

    companion object {
        const val DATABASE_NAME = "messages.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "messages"
        const val COLUMN_ID = "id"
        const val COLUMN_SENDER = "sender"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_IS_SENT = "is_sent"
    }
}