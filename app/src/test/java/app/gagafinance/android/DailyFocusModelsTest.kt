package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailyFocusModelsTest {
    private fun at(y: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            set(y, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun dailyCashflowSeparatesCurrentMonthPreviousAndFutureTransactions() {
        val now = at(2026, 11, 10)
        val entries = listOf(
            FinanceEntry(1, EntryKind.INCOME, "Pay", 50_000L, at(2026, 11, 10), "Salary"),
            FinanceEntry(2, EntryKind.EXPENSE, "Lunch", 2_000L, at(2026, 11, 10), "Food"),
            FinanceEntry(3, EntryKind.EXPENSE, "Shopping", 10_000L, at(2026, 11, 2), "Shopping"),
            FinanceEntry(4, EntryKind.INCOME, "Old", 99_000L, at(2026, 10, 20), "Salary"),
            FinanceEntry(5, EntryKind.EXPENSE, "Future", 99_000L, at(2026, 12, 2), "Food")
        )
        val result = DailyFocusMath.calculate(
            entries, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(50_000L, result.todayIncome)
        assertEquals(2_000L, result.todayExpense)
        assertEquals(50_000L, result.monthIncome)
        assertEquals(12_000L, result.monthExpense)
        assertEquals(38_000L, result.monthlyNet)
    }

    @Test
    fun alertsIgnorePaidBillsAndSettledLoansAndDoNotCountFutureItemsAsOverdue() {
        val now = at(2026, 11, 10)
        val overdue = BillReminder(1, "Rent", 30_000L, at(2026, 11, 8), null, "", now)
        val paid = BillReminder(2, "Wifi", 5_000L, at(2026, 11, 8), now, "", now)
        val soon = BillReminder(3, "Phone", 1_000L, at(2026, 11, 14), null, "", now)
        val later = BillReminder(4, "School", 2_000L, at(2026, 12, 10), null, "", now)
        val loans = listOf(
            LoanContract(1, 1, DebtEventKind.LENT, 5_000, 0, at(2026, 11, 7), "", now),
            LoanContract(2, 1, DebtEventKind.BORROWED, 8_000, 0, at(2026, 11, 12), "", now)
        )
        val status = DailyFocusMath.calculate(
            emptyList(), emptyList(), emptyList(),
            listOf(overdue, paid, soon, later), loans,
            listOf(LoanRepayment(1, 1, 5_000, now)), now
        )
        assertEquals(listOf(1L), status.overdueBills.map { it.id })
        assertEquals(listOf(3L), status.upcomingBills.map { it.id })
        assertTrue(status.overdueLoans.isEmpty())
        assertEquals(listOf(2L), status.upcomingLoans.map { it.id })
        assertEquals(1, status.urgentCount)
    }

    @Test
    fun warnsAboutApproachingMonthlyBudgetWithoutDoubleCountingAsOverdue() {
        val now = at(2026, 11, 10)
        val focus = DailyFocusMath.calculate(
            listOf(
                FinanceEntry(1, EntryKind.EXPENSE, "Food", 8_000, now, "Food"),
                FinanceEntry(2, EntryKind.EXPENSE, "Bus", 6_000, now, "Transport")
            ),
            listOf(MonthlyBudget("Food", 10_000), MonthlyBudget("Transport", 5_000)),
            emptyList(), emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(listOf("Food"), focus.nearBudgets.map { it.category })
        assertEquals(listOf("Transport"), focus.overBudgets.map { it.category })
        assertEquals(1, focus.urgentCount)
    }

    @Test
    fun filtersByWeekMonthTypeAndText() {
        val now = at(2026, 11, 10)
        val rows = listOf(
            FinanceEntry(1, EntryKind.EXPENSE, "Lunch", 5000, at(2026, 11, 8), "Food"),
            FinanceEntry(2, EntryKind.INCOME, "Salary", 100000, at(2026, 11, 2), "Salary"),
            FinanceEntry(3, EntryKind.EXPENSE, "Old lunch", 1000, at(2026, 10, 25), "Food")
        )
        assertEquals(listOf(1L),
            TransactionFilters.apply(rows, "", "Expense", TransactionFilters.WEEK, now)
                .map { it.id })
        assertEquals(listOf(1L, 2L),
            TransactionFilters.apply(rows, "", "All", TransactionFilters.MONTH, now)
                .map { it.id })
        assertEquals(listOf(1L, 3L),
            TransactionFilters.apply(rows, "lunch", "All", TransactionFilters.ALL, now)
                .map { it.id })
    }
}
