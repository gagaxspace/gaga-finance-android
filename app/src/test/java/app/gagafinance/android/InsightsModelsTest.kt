package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class InsightsModelsTest {
    @Test
    fun currentAndPreviousMonthsAreSeparatedAndCategorized() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 13, 0, 0)
        }.timeInMillis
        val (start, nextStart) = PlanningMath.monthRange(now)
        val lastStart = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.MONTH, -1)
        }.timeInMillis
        val entries = listOf(
            FinanceEntry(1, EntryKind.INCOME, "Pay", 12_000L, start, "Salary"),
            FinanceEntry(2, EntryKind.EXPENSE, "Meal", 3_000L, start, "Food"),
            FinanceEntry(3, EntryKind.EXPENSE, "Snack", 2_000L, start, "Food"),
            FinanceEntry(4, EntryKind.EXPENSE, "Bus", 1_000L, start, "Transport"),
            FinanceEntry(5, EntryKind.EXPENSE, "Old meal", 4_000L, lastStart, "Food"),
            FinanceEntry(6, EntryKind.EXPENSE, "Future meal", 7_000L, nextStart, "Food")
        )
        val summary = InsightsMath.summarize(entries, now)
        assertEquals(12_000L, summary.currentIncome)
        assertEquals(6_000L, summary.currentExpenses)
        assertEquals(6_000L, summary.currentNet)
        assertEquals(4_000L, summary.previousMonthExpenses)
        assertEquals(listOf("Food" to 5_000L, "Transport" to 1_000L), summary.categoryExpenses)
    }

    @Test
    fun emptyEntriesHaveZeroSummaries() {
        val report = InsightsMath.summarize(emptyList())
        assertEquals(0L, report.currentIncome)
        assertEquals(0L, report.currentExpenses)
        assertEquals(0L, report.previousMonthExpenses)
        assertEquals(emptyList<Pair<String, Long>>(), report.categoryExpenses)
    }
}
