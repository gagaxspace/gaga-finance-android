package app.gagafinance.android

import java.util.Calendar

/**
 * Home summaries are derived from saved, user-entered records.
 * Due items are in-app alerts only; there is no background alarm or payment.
 */
data class DailyFocus(
    val todayIncome: Long,
    val todayExpense: Long,
    val monthIncome: Long,
    val monthExpense: Long,
    val overdueBills: List<BillReminder>,
    val upcomingBills: List<BillReminder>,
    val overdueLoans: List<LoanContract>,
    val upcomingLoans: List<LoanContract>,
    val overBudgets: List<BudgetStatus>,
    val nearBudgets: List<BudgetStatus>,
    val goalsReached: Int
) {
    val monthlyNet: Long get() = Math.subtractExact(monthIncome, monthExpense)
    val urgentCount: Int get() = overdueBills.size + overdueLoans.size +
        overBudgets.size
}

object DailyFocusMath {
    fun dayStart(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun dateRange(start: Long, days: Int): Long {
        require(days != 0)
        return Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    fun calculate(
        entries: List<FinanceEntry>,
        budgets: List<MonthlyBudget>,
        goals: List<SavingsGoal>,
        bills: List<BillReminder>,
        loans: List<LoanContract>,
        repayments: List<LoanRepayment>,
        now: Long = System.currentTimeMillis()
    ): DailyFocus {
        val start = dayStart(now)
        val tomorrow = dateRange(start, 1)
        val upcomingEnd = dateRange(start, 8)
        val (monthStart, monthEnd) = PlanningMath.monthRange(now)

        fun sumWithin(kind: EntryKind, from: Long, until: Long): Long =
            entries.asSequence()
                .filter { it.kind == kind && it.createdAt >= from && it.createdAt < until }
                .fold(0L) { sum, item -> Math.addExact(sum, item.minorUnits) }

        val budgetStatuses = budgets.map { PlanningMath.budgetStatus(it, entries, now) }
        val unpaidBills = bills.filter { it.paidAt == null }
        val outstandingLoans = loans.filter {
            ObligationMath.loanRemaining(it, repayments) > 0L
        }
        return DailyFocus(
            todayIncome = sumWithin(EntryKind.INCOME, start, tomorrow),
            todayExpense = sumWithin(EntryKind.EXPENSE, start, tomorrow),
            monthIncome = sumWithin(EntryKind.INCOME, monthStart, monthEnd),
            monthExpense = sumWithin(EntryKind.EXPENSE, monthStart, monthEnd),
            overdueBills = unpaidBills.filter { it.dueAt < start }.sortedBy { it.dueAt },
            upcomingBills = unpaidBills.filter { it.dueAt >= start && it.dueAt < upcomingEnd }
                .sortedBy { it.dueAt },
            overdueLoans = outstandingLoans.filter { it.dueAt < start }.sortedBy { it.dueAt },
            upcomingLoans = outstandingLoans.filter { it.dueAt >= start && it.dueAt < upcomingEnd }
                .sortedBy { it.dueAt },
            overBudgets = budgetStatuses.filter { it.overBudget },
            nearBudgets = budgetStatuses.filter {
                !it.overBudget && it.limitMinorUnits > 0L &&
                    it.spentMinorUnits.toDouble() / it.limitMinorUnits.toDouble() >= 0.8
            },
            goalsReached = goals.count { it.isReached }
        )
    }
}

object TransactionFilters {
    const val ALL = "All time"
    const val MONTH = "This month"
    const val WEEK = "Last 7 days"

    fun apply(
        entries: List<FinanceEntry>, search: String, kind: String,
        dateFilter: String, now: Long = System.currentTimeMillis()
    ): List<FinanceEntry> {
        val start = when (dateFilter) {
            MONTH -> PlanningMath.monthRange(now).first
            WEEK -> DailyFocusMath.dateRange(DailyFocusMath.dayStart(now), 1 - 7)
            else -> Long.MIN_VALUE
        }
        val end = when (dateFilter) {
            MONTH -> PlanningMath.monthRange(now).second
            WEEK -> DailyFocusMath.dateRange(DailyFocusMath.dayStart(now), 1)
            else -> Long.MAX_VALUE
        }
        val trimmed = search.trim()
        return entries.filter { item ->
            item.createdAt >= start && item.createdAt < end &&
                (kind == "All" || item.kind.name.equals(kind, ignoreCase = true)) &&
                (item.description.contains(trimmed, ignoreCase = true) ||
                    item.category.contains(trimmed, ignoreCase = true) ||
                    Money.amountForCsv(item.minorUnits).contains(trimmed))
        }
    }
}
