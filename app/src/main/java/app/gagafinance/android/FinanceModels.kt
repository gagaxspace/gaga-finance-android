package app.gagafinance.android

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Entries are user-maintained records, NOT a wallet or bank balance. */
enum class EntryKind { INCOME, EXPENSE }

data class FinanceEntry(
    val id: Long,
    val kind: EntryKind,
    val description: String,
    val minorUnits: Long,
    val createdAt: Long
)

data class Cashflow(val income: Long = 0, val expenses: Long = 0) {
    val net: Long get() = income - expenses
}

object Money {
    /** Fixed-point BDT; never use floating-point values for stored amounts. */
    private val validAmount = Regex("[0-9]{1,9}(\\.[0-9]{1,2})?")

    fun parseMinorUnits(input: String): Long? {
        val normalized = input.trim()
        if (!validAmount.matches(normalized)) return null
        return try {
            BigDecimal(normalized).movePointRight(2).longValueExact().takeIf { it > 0 }
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun formatBDT(minorUnits: Long): String {
        val formatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        return "৳ " + formatter.format(BigDecimal.valueOf(minorUnits, 2))
    }

    fun amountForCsv(minorUnits: Long): String =
        BigDecimal.valueOf(minorUnits, 2).toPlainString()
}

object FinanceMath {
    fun monthStart(now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun totals(entries: List<FinanceEntry>, since: Long): Cashflow {
        var income = 0L
        var expenses = 0L
        for (entry in entries) {
            if (entry.createdAt < since) continue
            if (entry.kind == EntryKind.INCOME) {
                income = Math.addExact(income, entry.minorUnits)
            } else {
                expenses = Math.addExact(expenses, entry.minorUnits)
            }
        }
        return Cashflow(income, expenses)
    }
}

object FinanceCsv {
    fun export(entries: List<FinanceEntry>): String {
        val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return buildString {
            appendLine("timestamp_utc,type,description,amount_bdt")
            for (entry in entries.sortedBy { it.createdAt }) {
                append(time.format(Date(entry.createdAt)))
                append(',')
                append(entry.kind.name.lowercase(Locale.US))
                append(',')
                append(escapeCell(entry.description))
                append(',')
                append(Money.amountForCsv(entry.minorUnits))
                append('\n')
            }
        }
    }

    /** Prevent descriptions from turning into spreadsheet formulas on CSV import. */
    private fun escapeCell(input: String): String {
        val clean = input.replace("\r", " ").replace("\n", " ").trim()
        val safe = if (clean.startsWith("=") || clean.startsWith("+") ||
            clean.startsWith("-") || clean.startsWith("@") || clean.startsWith("\t")) {
            "'" + clean
        } else {
            clean
        }
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
