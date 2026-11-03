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
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

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
        createPeopleTables(db)
        createPlanningTables(db)
    }

    private fun createPeopleTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ledger_people (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ledger_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                person_id INTEGER NOT NULL REFERENCES ledger_people(id) ON DELETE RESTRICT,
                kind TEXT NOT NULL CHECK(kind IN ('LENT', 'BORROWED', 'REPAID_TO_ME', 'I_REPAID')),
                minor_units INTEGER NOT NULL CHECK(minor_units > 0),
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS ledger_events_by_person " +
                "ON ledger_events(person_id, created_at, id)"
        )
    }

    private fun createPlanningTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS monthly_budgets (
                category TEXT PRIMARY KEY NOT NULL,
                limit_minor_units INTEGER NOT NULL CHECK(limit_minor_units > 0)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_minor_units INTEGER NOT NULL CHECK(target_minor_units > 0),
                saved_minor_units INTEGER NOT NULL DEFAULT 0 CHECK(saved_minor_units >= 0),
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS savings_changes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id INTEGER NOT NULL REFERENCES savings_goals(id) ON DELETE RESTRICT,
                kind TEXT NOT NULL CHECK(kind IN ('ADD', 'WITHDRAW')),
                minor_units INTEGER NOT NULL CHECK(minor_units > 0),
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS savings_changes_by_goal " +
                "ON savings_changes(goal_id, created_at, id)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..newVersion && newVersion == DATABASE_VERSION) {
            "Unsupported database migration from " + oldVersion + " to " + newVersion
        }
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
        }
        if (oldVersion < 3) {
            createPeopleTables(db)
        }
        if (oldVersion < 4) {
            createPlanningTables(db)
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

    fun listPeople(): List<LedgerPerson> {
        val result = mutableListOf<LedgerPerson>()
        readableDatabase.query(
            "ledger_people", arrayOf("id", "name", "note", "created_at"),
            null, null, null, null, "name COLLATE NOCASE ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    LedgerPerson(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        note = cursor.getString(2),
                        createdAt = cursor.getLong(3)
                    )
                )
            }
        }
        return result
    }

    fun listDebtEvents(): List<DebtEvent> = queryDebtEvents(readableDatabase, null)

    private fun queryDebtEvents(db: SQLiteDatabase, personId: Long?): List<DebtEvent> {
        val result = mutableListOf<DebtEvent>()
        val where = if (personId == null) null else "person_id = ?"
        val args = if (personId == null) null else arrayOf(personId.toString())
        db.query(
            "ledger_events",
            arrayOf("id", "person_id", "kind", "minor_units", "note", "created_at"),
            where, args, null, null, "created_at ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    DebtEvent(
                        id = cursor.getLong(0),
                        personId = cursor.getLong(1),
                        kind = DebtEventKind.valueOf(cursor.getString(2)),
                        minorUnits = cursor.getLong(3),
                        note = cursor.getString(4),
                        createdAt = cursor.getLong(5)
                    )
                )
            }
        }
        return result
    }

    fun addPerson(name: String, note: String = ""): Long {
        val cleanName = name.trim()
        val cleanNote = note.trim()
        require(cleanName.isNotBlank() && cleanName.length <= 80) {
            "Name must contain 1–80 characters"
        }
        require(cleanNote.length <= 160) { "Note must be at most 160 characters" }
        val values = ContentValues().apply {
            put("name", cleanName)
            put("note", cleanNote)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("ledger_people", null, values)
    }

    /**
     * Record a manually entered lending/borrowing event, not a payment.
     * Outstanding repayments are validated atomically with the write.
     * History is append-only to avoid silently changing recorded debts.
     */
    fun addDebtEvent(personId: Long, kind: DebtEventKind, minorUnits: Long, note: String): Long {
        require(personId > 0)
        require(minorUnits > 0)
        val cleanNote = note.trim()
        require(cleanNote.length <= 160) { "Note must be at most 160 characters" }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val personExists = db.query(
                "ledger_people", arrayOf("id"), "id = ?",
                arrayOf(personId.toString()), null, null, null
            ).use { it.moveToFirst() }
            require(personExists) { "This person no longer exists" }
            val priorEvents = queryDebtEvents(db, personId)
            val balance = DebtMath.balance(priorEvents)
            require(DebtMath.canAdd(balance, kind, minorUnits)) {
                "Repayment exceeds the outstanding amount"
            }
            val now = System.currentTimeMillis()
            // Check future arithmetic for overflow before inserting.
            DebtMath.balance(
                priorEvents + DebtEvent(
                    id = Long.MAX_VALUE, personId = personId, kind = kind,
                    minorUnits = minorUnits, note = cleanNote, createdAt = now
                )
            )
            val values = ContentValues().apply {
                put("person_id", personId)
                put("kind", kind.name)
                put("minor_units", minorUnits)
                put("note", cleanNote)
                put("created_at", now)
            }
            val newId = db.insertOrThrow("ledger_events", null, values)
            db.setTransactionSuccessful()
            newId
        } finally {
            db.endTransaction()
        }
    }

    fun listBudgets(): List<MonthlyBudget> {
        val result = mutableListOf<MonthlyBudget>()
        readableDatabase.query(
            "monthly_budgets", arrayOf("category", "limit_minor_units"),
            null, null, null, null, "category COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(MonthlyBudget(cursor.getString(0), cursor.getLong(1)))
            }
        }
        return result
    }

    fun upsertBudget(category: String, limitMinorUnits: Long) {
        require(FinanceCategories.valid(category))
        require(limitMinorUnits > 0L)
        val values = ContentValues().apply {
            put("category", category)
            put("limit_minor_units", limitMinorUnits)
        }
        writableDatabase.insertWithOnConflict(
            "monthly_budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE
        ).also { check(it != -1L) { "Budget not saved" } }
    }

    fun deleteBudget(category: String): Boolean {
        require(FinanceCategories.valid(category))
        return writableDatabase.delete(
            "monthly_budgets", "category = ?", arrayOf(category)
        ) == 1
    }

    fun listSavingsGoals(): List<SavingsGoal> {
        val result = mutableListOf<SavingsGoal>()
        readableDatabase.query(
            "savings_goals",
            arrayOf("id", "name", "target_minor_units", "saved_minor_units", "created_at"),
            null, null, null, null, "created_at DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    SavingsGoal(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        targetMinorUnits = cursor.getLong(2),
                        savedMinorUnits = cursor.getLong(3),
                        createdAt = cursor.getLong(4)
                    )
                )
            }
        }
        return result
    }

    fun createSavingsGoal(name: String, targetMinorUnits: Long): Long {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 80)
        require(targetMinorUnits > 0L)
        val values = ContentValues().apply {
            put("name", clean)
            put("target_minor_units", targetMinorUnits)
            put("saved_minor_units", 0L)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("savings_goals", null, values)
    }

    /** Record manual progress; no funds are actually moved or reserved. */
    fun recordSavingsChange(
        goalId: Long, kind: SavingsChangeKind, amountMinorUnits: Long
    ): Long {
        require(goalId > 0L && amountMinorUnits > 0L)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val previous = db.query(
                "savings_goals", arrayOf("saved_minor_units"),
                "id = ?", arrayOf(goalId.toString()), null, null, null
            ).use { cursor ->
                require(cursor.moveToFirst()) { "Savings goal no longer exists" }
                cursor.getLong(0)
            }
            val next = PlanningMath.balanceAfterChange(previous, kind, amountMinorUnits)
            val values = ContentValues().apply { put("saved_minor_units", next) }
            check(db.update(
                "savings_goals", values, "id = ?", arrayOf(goalId.toString())
            ) == 1)
            val record = ContentValues().apply {
                put("goal_id", goalId)
                put("kind", kind.name)
                put("minor_units", amountMinorUnits)
                put("created_at", System.currentTimeMillis())
            }
            val eventId = db.insertOrThrow("savings_changes", null, record)
            db.setTransactionSuccessful()
            eventId
        } finally {
            db.endTransaction()
        }
    }

    fun listSavingsChanges(): List<SavingsChange> {
        val result = mutableListOf<SavingsChange>()
        readableDatabase.query(
            "savings_changes",
            arrayOf("id", "goal_id", "kind", "minor_units", "created_at"),
            null, null, null, null, "created_at DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    SavingsChange(
                        id = cursor.getLong(0),
                        goalId = cursor.getLong(1),
                        kind = SavingsChangeKind.valueOf(cursor.getString(2)),
                        minorUnits = cursor.getLong(3),
                        createdAt = cursor.getLong(4)
                    )
                )
            }
        }
        return result
    }

    companion object { private const val DATABASE_VERSION = 4 }
}
