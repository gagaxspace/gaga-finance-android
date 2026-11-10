package app.gagafinance.android

import java.util.Calendar

data class MonthInsight(
    val currentIncome: Long,
    val currentExpenses: Long,
    val previousMonthExpenses: Long,
    val categoryExpenses: List<Pair<String, Long>>
) {
    val currentNet: Long get() = Math.subtractExact(currentIncome, currentExpenses)
}

object InsightsMath {
    /** Summarize manually recorded transactions only; no external data or AI calls. */
    fun summarize(
        entries: List<FinanceEntry>,
        now: Long = System.currentTimeMillis()
    ): MonthInsight {
        val (monthStart, nextMonthStart) = PlanningMath.monthRange(now)
        val previousMonthStart = Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, -1)
        }.timeInMillis
        var income = 0L
        var expenses = 0L
        var previousExpenses = 0L
        val categories = mutableMapOf<String, Long>()

        for (entry in entries) {
            if (entry.createdAt >= monthStart && entry.createdAt < nextMonthStart) {
                if (entry.kind == EntryKind.INCOME) {
                    income = Math.addExact(income, entry.minorUnits)
                } else {
                    expenses = Math.addExact(expenses, entry.minorUnits)
                    val existing = categories[entry.category] ?: 0L
                    categories[entry.category] = Math.addExact(existing, entry.minorUnits)
                }
            } else if (entry.kind == EntryKind.EXPENSE &&
                entry.createdAt >= previousMonthStart &&
                entry.createdAt < monthStart
            ) {
                previousExpenses = Math.addExact(previousExpenses, entry.minorUnits)
            }
        }
        return MonthInsight(
            currentIncome = income,
            currentExpenses = expenses,
            previousMonthExpenses = previousExpenses,
            categoryExpenses = categories.toList().sortedWith(
                compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first }
            )
        )
    }
}
