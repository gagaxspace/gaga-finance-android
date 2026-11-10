package app.gagafinance.android

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A standalone manual loan contract; not added again to People Ledger balances. */
data class LoanContract(
    val id: Long,
    val personId: Long,
    val direction: DebtEventKind,
    val principalMinor: Long,
    val annualInterestBps: Int,
    val dueAt: Long,
    val note: String,
    val createdAt: Long
)

data class LoanRepayment(
    val id: Long,
    val loanId: Long,
    val minorUnits: Long,
    val createdAt: Long
)

data class BillReminder(
    val id: Long,
    val name: String,
    val minorUnits: Long,
    val dueAt: Long,
    val paidAt: Long?,
    val note: String,
    val createdAt: Long
)

object FinanceDates {
    private fun formatter(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    fun parseDate(text: String): Long? =
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(text.trim())) null
        else try { formatter().parse(text.trim())?.time } catch (_: Exception) { null }

    fun formatDate(millis: Long): String = formatter().format(Date(millis))

    fun isPastDay(dueAt: Long, now: Long = System.currentTimeMillis()): Boolean {
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dueAt < today
    }
}

object ObligationMath {
    fun loanRemaining(loan: LoanContract, repayments: List<LoanRepayment>): Long =
        Math.subtractExact(
            loan.principalMinor,
            repayments.filter { it.loanId == loan.id }.fold(0L) { sum, record ->
                Math.addExact(sum, record.minorUnits)
            }
        ).also { require(it >= 0L) { "Loan repayments exceed principal" } }

    /** Simple annual interest estimate only, not an actual payment obligation. */
    fun estimatedInterest(loan: LoanContract): Long {
        val days = ((loan.dueAt - loan.createdAt) / 86_400_000L).coerceAtLeast(0)
        val numerator = BigDecimal.valueOf(loan.principalMinor)
            .multiply(BigDecimal.valueOf(loan.annualInterestBps.toLong()))
            .multiply(BigDecimal.valueOf(days))
        return numerator.divide(
            BigDecimal.valueOf(3_650_000L), 0, RoundingMode.HALF_UP
        ).longValueExact()
    }

    fun status(loan: LoanContract, repayments: List<LoanRepayment>, now: Long): String =
        when {
            loanRemaining(loan, repayments) == 0L -> "Settled"
            FinanceDates.isPastDay(loan.dueAt, now) -> "Overdue"
            else -> "Active"
        }

    fun billStatus(bill: BillReminder, now: Long): String =
        when {
            bill.paidAt != null -> "Paid"
            FinanceDates.isPastDay(bill.dueAt, now) -> "Overdue"
            else -> "Pending"
        }

    fun totalOutstanding(
        loans: List<LoanContract>, repayments: List<LoanRepayment>, direction: DebtEventKind
    ): Long = loans.filter { it.direction == direction }.fold(0L) { sum, loan ->
        Math.addExact(sum, loanRemaining(loan, repayments))
    }
}
