package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PlanningModelsTest {
    @Test
    fun budgetCountsOnlyMatchingExpensesWithinCurrentMonth() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }.timeInMillis
        val (begin, end) = PlanningMath.monthRange(now)
        val budget = MonthlyBudget("Food", 500_00L)
        val entries = listOf(
            FinanceEntry(1, EntryKind.EXPENSE, "Meal", 200_00L, begin, "Food"),
            FinanceEntry(2, EntryKind.INCOME, "Refund income", 100_00L, begin, "Food"),
            FinanceEntry(3, EntryKind.EXPENSE, "Bus", 100_00L, begin, "Transport"),
            FinanceEntry(4, EntryKind.EXPENSE, "October", 100_00L, begin - 1, "Food"),
            FinanceEntry(5, EntryKind.EXPENSE, "December", 100_00L, end, "Food")
        )
        val status = PlanningMath.budgetStatus(budget, entries, now)
        assertEquals(200_00L, status.spentMinorUnits)
        assertEquals(300_00L, status.remainingMinorUnits)
        assertFalse(status.overBudget)
        assertEquals(40, status.percentUsed)
    }

    @Test
    fun budgetCorrectlyReportsOverspend() {
        val now = System.currentTimeMillis()
        val status = PlanningMath.budgetStatus(
            MonthlyBudget("Food", 100_00L),
            listOf(
                FinanceEntry(
                    1, EntryKind.EXPENSE, "Meal", 150_00L, now, "Food"
                )
            ), now
        )
        assertTrue(status.overBudget)
        assertEquals(0L, status.remainingMinorUnits)
        assertEquals(150, status.percentUsed)
    }

    @Test
    fun savingsGoalsAreManualAndCannotGoNegative() {
        assertEquals(4_000L, PlanningMath.balanceAfterChange(2_500L, SavingsChangeKind.ADD, 1_500L))
        assertEquals(0L, PlanningMath.balanceAfterChange(2_500L, SavingsChangeKind.WITHDRAW, 2_500L))
        assertEquals(0.5f, PlanningMath.progress(5_000L, 10_000L), 0.00001f)
        assertEquals(1f, PlanningMath.progress(20_000L, 10_000L), 0.00001f)
        val goal = SavingsGoal(1, "Laptop", 10_000L, 12_000L, 0L)
        assertTrue(goal.isReached)
        assertEquals(0L, goal.remainingMinorUnits)
    }

    @Test
    fun planningExportKeepsGoalHistoryAndEscapesDangerousNames() {
        val text = PlanningCsv.export(
            listOf(MonthlyBudget("Food", 25_000L)),
            listOf(SavingsGoal(7L, "=formula", 100_000L, 20_000L, 0L)),
            listOf(SavingsChange(11L, 7L, SavingsChangeKind.ADD, 20_000L, 1000L))
        )
        assertTrue(text.contains("budget,,\"Food\",250.00"))
        assertTrue(text.contains("\"'=formula\""))
        assertTrue(text.contains("goal,7,"))
        assertTrue(text.contains("goal_change,7"))
        assertTrue(text.contains(",add,200.00,"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun savingsCannotWithdrawAboveRecordedAmount() {
        PlanningMath.balanceAfterChange(2_500L, SavingsChangeKind.WITHDRAW, 2_501L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroProgressChangesAreRejected() {
        PlanningMath.balanceAfterChange(2_500L, SavingsChangeKind.ADD, 0L)
    }
}
