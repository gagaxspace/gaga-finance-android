package app.gagafinance.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * A device-only personal bookkeeping store. It has no account connection, payment
 * authority, cloud sync, or server ledger. Keep it separate from any future wallet.
 */
class LocalFinanceStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "gaga-finance-local.db", null, DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL CHECK(kind IN ('INCOME', 'EXPENSE')),
                description TEXT NOT NULL,
                minor_units INTEGER NOT NULL CHECK(minor_units > 0),
                created_at INTEGER NOT NULL,
                category TEXT NOT NULL DEFAULT 'Other'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX entries_by_created_at ON entries(created_at DESC, id DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
        } else {
            error("No migration from database version " + oldVersion + " to " + newVersion)
        }
    }

    fun listEntries(): List<FinanceEntry> {
        val result = mutableListOf<FinanceEntry>()
        readableDatabase.query(
            "entries",
            arrayOf("id", "kind", "description", "minor_units", "created_at", "category"),
            null, null, null, null, "created_at DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(FinanceEntry(
                    id = cursor.getLong(0),
                    kind = EntryKind.valueOf(cursor.getString(1)),
                    description = cursor.getString(2),
                    minorUnits = cursor.getLong(3),
                    createdAt = cursor.getLong(4),
                    category = cursor.getString(5)
                ))
            }
        }
        return result
    }

    fun addEntry(kind: EntryKind, description: String, minorUnits: Long, category: String = "Other"): Long {
        val clean = description.trim()
        require(clean.isNotEmpty() && clean.length <= 80) { "Description must be 1–80 characters" }
        require(minorUnits > 0) { "Amount must be positive" }
        require(FinanceCategories.valid(category)) { "Invalid transaction category" }
        val values = ContentValues().apply {
            put("kind", kind.name)
            put("description", clean)
            put("minor_units", minorUnits)
            put("created_at", System.currentTimeMillis())
            put("category", category)
        }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val id = db.insertOrThrow("entries", null, values)
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    /** Update the entry in place, preserving its ID and original date. */
    fun updateEntry(
        id: Long, kind: EntryKind, description: String,
        minorUnits: Long, category: String
    ): Boolean {
        val clean = description.trim()
        require(id > 0)
        require(clean.isNotEmpty() && clean.length <= 80)
        require(minorUnits > 0)
        require(FinanceCategories.valid(category))
        val values = ContentValues().apply {
            put("kind", kind.name)
            put("description", clean)
            put("minor_units", minorUnits)
            put("category", category)
        }
        return writableDatabase.update("entries", values, "id = ?", arrayOf(id.toString())) == 1
    }

    fun deleteEntry(id: Long): Boolean {
        require(id > 0)
        return writableDatabase.delete("entries", "id = ?", arrayOf(id.toString())) == 1
    }

    companion object { private const val DATABASE_VERSION = 2 }
}
