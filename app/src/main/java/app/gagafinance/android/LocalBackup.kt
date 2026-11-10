package app.gagafinance.android

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(val recordCounts: Map<String, Int>) {
    val total: Int get() = recordCounts.values.sum()
}

/**
 * Versioned manual backup. A restore is atomic and validates full FK integrity
 * before replacing local data. This is plaintext: exported files are sensitive.
 */
object LocalBackup {
    private const val FORMAT = "gaga-finance-local-backup"
    private const val VERSION = 1
    private const val MAX_BYTES = 8_000_000
    private const val MAX_ROWS = 50_000

    private data class Table(
        val name: String,
        val longs: List<String>,
        val strings: List<String>,
        val nullableLongs: List<String> = emptyList()
    ) {
        val columns: List<String> get() = longs + strings + nullableLongs
    }

    // Parent-before-child order; never use an imported table/column name as SQL.
    private val tables = listOf(
        Table("entries", listOf("id", "minor_units", "created_at"),
            listOf("kind", "description", "category")),
        Table("ledger_people", listOf("id", "created_at"),
            listOf("name", "note", "phone", "email", "address", "relation")),
        Table("ledger_events", listOf("id", "person_id", "minor_units", "created_at"),
            listOf("kind", "note")),
        Table("monthly_budgets", listOf("limit_minor_units"), listOf("category")),
        Table("savings_goals",
            listOf("id", "target_minor_units", "saved_minor_units", "created_at"),
            listOf("name")),
        Table("savings_changes",
            listOf("id", "goal_id", "minor_units", "created_at"),
            listOf("kind")),
        Table("loan_contracts",
            listOf("id", "person_id", "principal_minor", "annual_interest_bps", "due_at", "created_at"),
            listOf("direction", "note")),
        Table("loan_repayments",
            listOf("id", "loan_id", "minor_units", "created_at"), emptyList()),
        Table("bill_reminders",
            listOf("id", "minor_units", "due_at", "created_at"),
            listOf("name", "note"), listOf("paid_at"))
    )

    fun export(db: SQLiteDatabase): String {
        val data = JSONObject()
        for (table in tables) {
            val rows = JSONArray()
            db.query(
                table.name, table.columns.toTypedArray(),
                null, null, null, null, null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (i in table.columns.indices) {
                        val value = when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                            else -> error("Unsupported local field type")
                        }
                        row.put(table.columns[i], value)
                    }
                    rows.put(row)
                }
            }
            data.put(table.name, rows)
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("tables", data)
            .toString(2)
    }

    private fun validated(raw: String): JSONObject {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Backup exceeds 8 MB safety limit"
        }
        val root = try { JSONObject(raw) } catch (_: Exception) {
            throw IllegalArgumentException("Invalid JSON backup")
        }
        require(root.optString("format") == FORMAT && root.optInt("version") == VERSION) {
            "This is not a compatible GaGa Finance local backup"
        }
        val data = root.optJSONObject("tables")
            ?: throw IllegalArgumentException("Backup has no tables")
        var rowCount = 0
        for (table in tables) {
            val rows = data.optJSONArray(table.name)
                ?: throw IllegalArgumentException("Backup missing " + table.name)
            rowCount = Math.addExact(rowCount, rows.length())
            require(rowCount <= MAX_ROWS) { "Too many backup records" }
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i)
                    ?: throw IllegalArgumentException("Invalid row in " + table.name)
                for (column in table.columns) {
                    require(row.has(column)) { "Missing field " + table.name + "." + column }
                    val value = row.get(column)
                    when (column) {
                        in table.longs -> require(value is Number && value !is Float && value !is Double) {
                            "Expected integer for " + column
                        }
                        in table.strings -> require(value is String && value.length <= 4096) {
                            "Invalid text in " + column
                        }
                        in table.nullableLongs -> require(
                            value == JSONObject.NULL || (value is Number && value !is Float && value !is Double)
                        ) { "Invalid optional timestamp" }
                    }
                }
                val keys = row.keys()
                while (keys.hasNext()) {
                    require(keys.next() in table.columns) {
                        "Unexpected backup column"
                    }
                }
            }
        }
        val keys = data.keys()
        while (keys.hasNext()) {
            require(tables.any { it.name == keys.next() }) { "Unexpected backup table" }
        }
        return data
    }

    fun preview(raw: String): BackupPreview {
        val tablesObject = validated(raw)
        return BackupPreview(tables.associate {
            it.name to tablesObject.getJSONArray(it.name).length()
        })
    }

    fun restore(db: SQLiteDatabase, raw: String) {
        val data = validated(raw) // before touching current user data
        db.beginTransaction()
        try {
            // Delete children before parents; do not disable foreign keys.
            for (table in tables.asReversed()) db.delete(table.name, null, null)
            for (table in tables) {
                val rows = data.getJSONArray(table.name)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val values = ContentValues()
                    for (field in table.longs) {
                        values.put(field, row.getLong(field))
                    }
                    for (field in table.strings) {
                        values.put(field, row.getString(field))
                    }
                    for (field in table.nullableLongs) {
                        if (row.isNull(field)) values.putNull(field)
                        else values.put(field, row.getLong(field))
                    }
                    db.insertOrThrow(table.name, null, values)
                }
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                require(!cursor.moveToFirst()) { "Backup has broken record relationships" }
            }
            checkBusinessRules(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun checkBusinessRules(db: SQLiteDatabase) {
        // Repayments must be consistent with the append-only People Ledger.
        val events = mutableMapOf<Long, MutableList<DebtEvent>>()
        db.query(
            "ledger_events",
            arrayOf("id", "person_id", "kind", "minor_units", "note", "created_at"),
            null, null, null, null, "created_at ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val item = DebtEvent(
                    cursor.getLong(0), cursor.getLong(1),
                    DebtEventKind.valueOf(cursor.getString(2)),
                    cursor.getLong(3), cursor.getString(4), cursor.getLong(5)
                )
                events.getOrPut(item.personId) { mutableListOf() }.add(item)
            }
        }
        events.values.forEach { DebtMath.balance(it) }

        val loanPrincipal = mutableMapOf<Long, Long>()
        db.query("loan_contracts", arrayOf("id", "principal_minor"),
            null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) loanPrincipal[cursor.getLong(0)] = cursor.getLong(1)
        }
        val paid = mutableMapOf<Long, Long>()
        db.query("loan_repayments", arrayOf("loan_id", "minor_units"),
            null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                paid[id] = Math.addExact(paid[id] ?: 0L, cursor.getLong(1))
                require(paid.getValue(id) <= (loanPrincipal[id] ?: 0L)) {
                    "Loan repayments exceed principal"
                }
            }
        }

        val goals = mutableMapOf<Long, Long>()
        db.query("savings_goals", arrayOf("id", "saved_minor_units"),
            null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) goals[cursor.getLong(0)] = cursor.getLong(1)
        }
        val progress = mutableMapOf<Long, Long>()
        db.query(
            "savings_changes",
            arrayOf("goal_id", "kind", "minor_units"),
            null, null, null, null, "created_at ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val kind = SavingsChangeKind.valueOf(cursor.getString(1))
                progress[id] = PlanningMath.balanceAfterChange(
                    progress[id] ?: 0L, kind, cursor.getLong(2)
                )
            }
        }
        for ((id, saved) in goals) {
            require(progress.getOrDefault(id, 0L) == saved) {
                "Savings goal progress does not match its history"
            }
        }
    }

    fun clearAll(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            for (table in tables.asReversed()) db.delete(table.name, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
