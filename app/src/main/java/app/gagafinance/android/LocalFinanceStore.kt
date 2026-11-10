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
        addPeopleContactColumns(db)
        createPlanningTables(db)
        createExtrasTables(db)
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

    private fun addPeopleContactColumns(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE ledger_people ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE ledger_people ADD COLUMN email TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE ledger_people ADD COLUMN address TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE ledger_people ADD COLUMN relation TEXT NOT NULL DEFAULT ''")
    }

    private fun createExtrasTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS loan_contracts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                person_id INTEGER NOT NULL REFERENCES ledger_people(id) ON DELETE RESTRICT,
                direction TEXT NOT NULL CHECK(direction IN ('LENT', 'BORROWED')),
                principal_minor INTEGER NOT NULL CHECK(principal_minor > 0),
                annual_interest_bps INTEGER NOT NULL DEFAULT 0 CHECK(annual_interest_bps BETWEEN 0 AND 10000),
                due_at INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS loan_repayments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id INTEGER NOT NULL REFERENCES loan_contracts(id) ON DELETE RESTRICT,
                minor_units INTEGER NOT NULL CHECK(minor_units > 0),
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bill_reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                minor_units INTEGER NOT NULL CHECK(minor_units > 0),
                due_at INTEGER NOT NULL,
                paid_at INTEGER,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS bills_by_due ON bill_reminders(due_at, id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS loan_repayments_by_loan ON loan_repayments(loan_id, id)"
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
        if (oldVersion < 5) {
            addPeopleContactColumns(db)
            createExtrasTables(db)
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
            "ledger_people", arrayOf(
                "id", "name", "note", "created_at", "phone", "email", "address", "relation"
            ),
            null, null, null, null, "name COLLATE NOCASE ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    LedgerPerson(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        note = cursor.getString(2),
                        createdAt = cursor.getLong(3),
                        phone = cursor.getString(4),
                        email = cursor.getString(5),
                        address = cursor.getString(6),
                        relation = cursor.getString(7)
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

    fun updatePerson(profile: LedgerPerson): Boolean {
        require(profile.id > 0L)
        require(profile.name.isNotBlank() && profile.name.trim().length <= 80)
        require(profile.note.length <= 160)
        require(profile.phone.length <= 32)
        require(profile.email.length <= 160)
        require(profile.address.length <= 200)
        require(profile.relation.length <= 80)
        val fields = ContentValues().apply {
            put("name", profile.name.trim())
            put("note", profile.note.trim())
            put("phone", profile.phone.trim())
            put("email", profile.email.trim())
            put("address", profile.address.trim())
            put("relation", profile.relation.trim())
        }
        return writableDatabase.update(
            "ledger_people", fields, "id = ?", arrayOf(profile.id.toString())
        ) == 1
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
        require(FinanceCategories.valid(category) || category == "Overall")
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
        require(FinanceCategories.valid(category) || category == "Overall")
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

    fun listLoans(): List<LoanContract> {
        val records = mutableListOf<LoanContract>()
        readableDatabase.query(
            "loan_contracts",
            arrayOf(
                "id", "person_id", "direction", "principal_minor",
                "annual_interest_bps", "due_at", "note", "created_at"
            ),
            null, null, null, null, "due_at ASC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records.add(
                    LoanContract(
                        id = cursor.getLong(0), personId = cursor.getLong(1),
                        direction = DebtEventKind.valueOf(cursor.getString(2)),
                        principalMinor = cursor.getLong(3),
                        annualInterestBps = cursor.getInt(4),
                        dueAt = cursor.getLong(5),
                        note = cursor.getString(6),
                        createdAt = cursor.getLong(7)
                    )
                )
            }
        }
        return records
    }

    fun listLoanRepayments(): List<LoanRepayment> {
        val records = mutableListOf<LoanRepayment>()
        readableDatabase.query(
            "loan_repayments",
            arrayOf("id", "loan_id", "minor_units", "created_at"),
            null, null, null, null, "created_at ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records.add(
                    LoanRepayment(
                        id = cursor.getLong(0),
                        loanId = cursor.getLong(1),
                        minorUnits = cursor.getLong(2),
                        createdAt = cursor.getLong(3)
                    )
                )
            }
        }
        return records
    }

    fun createLoan(
        personId: Long, direction: DebtEventKind, principalMinor: Long,
        interestBps: Int, dueAt: Long, note: String
    ): Long {
        require(personId > 0 && principalMinor > 0)
        require(direction == DebtEventKind.LENT || direction == DebtEventKind.BORROWED)
        require(interestBps in 0..10_000)
        require(dueAt >= FinanceDates.parseDate("2000-01-01")!!)
        require(note.length <= 160)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val personExists = db.query(
                "ledger_people", arrayOf("id"), "id=?",
                arrayOf(personId.toString()), null, null, null
            ).use { it.moveToFirst() }
            require(personExists) { "Create the person before adding a loan" }
            val values = ContentValues().apply {
                put("person_id", personId)
                put("direction", direction.name)
                put("principal_minor", principalMinor)
                put("annual_interest_bps", interestBps)
                put("due_at", dueAt)
                put("note", note.trim())
                put("created_at", System.currentTimeMillis())
            }
            val id = db.insertOrThrow("loan_contracts", null, values)
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    /** Loan plan repayments reduce only the matching loan's principal. */
    fun recordLoanRepayment(loanId: Long, amountMinor: Long): Long {
        require(loanId > 0L && amountMinor > 0L)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val loan = db.query(
                "loan_contracts", arrayOf("principal_minor"), "id=?",
                arrayOf(loanId.toString()), null, null, null
            ).use { cursor ->
                require(cursor.moveToFirst()) { "Loan record not found" }
                cursor.getLong(0)
            }
            val repaid = db.query(
                "loan_repayments", arrayOf("minor_units"), "loan_id=?",
                arrayOf(loanId.toString()), null, null, null
            ).use { cursor ->
                var total = 0L
                while (cursor.moveToNext()) {
                    total = Math.addExact(total, cursor.getLong(0))
                }
                total
            }
            require(amountMinor <= Math.subtractExact(loan, repaid)) {
                "Repayment exceeds remaining loan principal"
            }
            val fields = ContentValues().apply {
                put("loan_id", loanId)
                put("minor_units", amountMinor)
                put("created_at", System.currentTimeMillis())
            }
            val id = db.insertOrThrow("loan_repayments", null, fields)
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    fun listBills(): List<BillReminder> {
        val result = mutableListOf<BillReminder>()
        readableDatabase.query(
            "bill_reminders",
            arrayOf("id", "name", "minor_units", "due_at", "paid_at", "note", "created_at"),
            null, null, null, null, "due_at ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    BillReminder(
                        id = cursor.getLong(0), name = cursor.getString(1),
                        minorUnits = cursor.getLong(2), dueAt = cursor.getLong(3),
                        paidAt = if (cursor.isNull(4)) null else cursor.getLong(4),
                        note = cursor.getString(5), createdAt = cursor.getLong(6)
                    )
                )
            }
        }
        return result
    }

    fun createBill(name: String, amountMinor: Long, dueAt: Long, note: String): Long {
        require(name.isNotBlank() && name.length <= 80)
        require(amountMinor > 0 && note.length <= 160)
        val values = ContentValues().apply {
            put("name", name.trim())
            put("minor_units", amountMinor)
            put("due_at", dueAt)
            put("note", note.trim())
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("bill_reminders", null, values)
    }

    fun setBillPaid(billId: Long, paid: Boolean): Boolean {
        require(billId > 0)
        val values = ContentValues().apply {
            if (paid) put("paid_at", System.currentTimeMillis())
            else putNull("paid_at")
        }
        return writableDatabase.update(
            "bill_reminders", values, "id=?", arrayOf(billId.toString())
        ) == 1
    }

    fun deleteBill(billId: Long): Boolean {
        require(billId > 0)
        return writableDatabase.delete(
            "bill_reminders", "id=?", arrayOf(billId.toString())
        ) == 1
    }

    fun exportJson(): String = LocalBackup.export(readableDatabase)

    fun previewImport(raw: String): BackupPreview = LocalBackup.preview(raw)

    fun restoreJson(raw: String) = LocalBackup.restore(writableDatabase, raw)

    fun clearAllRecords() = LocalBackup.clearAll(writableDatabase)

    companion object { private const val DATABASE_VERSION = 5 }
}
