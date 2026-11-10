package app.gagafinance.android

import java.util.Calendar

/** User-entered spending limit; it does not constrain real payments. */
data class MonthlyBudget(val category: String, val limitMinorUnits: Long)

data class BudgetStatus(
    val category: String,
    val limitMinorUnits: Long,
    val spentMinorUnits: Long
) {
    val remainingMinorUnits: Long get() = (limitMinorUnits - spentMinorUnits).coerceAtLeast(0L)
    val overBudget: Boolean get() = spentMinorUnits > limitMinorUnits
    val percentUsed: Int get() =
        if (limitMinorUnits == 0L) 0
        else ((spentMinorUnits.toDouble() / limitMinorUnits.toDouble()) * 100.0)
            .coerceIn(0.0, 10000.0).toInt()
}

data class SavingsGoal(
    val id: Long,
    val name: String,
    val targetMinorUnits: Long,
    val savedMinorUnits: Long,
    val createdAt: Long
) {
    val remainingMinorUnits: Long get() =
        (targetMinorUnits - savedMinorUnits).coerceAtLeast(0L)
    val isReached: Boolean get() = savedMinorUnits >= targetMinorUnits
}

enum class SavingsChangeKind { ADD, WITHDRAW }

data class SavingsChange(
    val id: Long,
    val goalId: Long,
    val kind: SavingsChangeKind,
    val minorUnits: Long,
    val createdAt: Long
)

object PlanningMath {
    fun monthRange(now: Long): Pair<Long, Long> {
        val start = FinanceMath.monthStart(now)
        val next = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return start to next
    }

    fun budgetStatus(
        budget: MonthlyBudget,
        entries: List<FinanceEntry>,
        now: Long = System.currentTimeMillis()
    ): BudgetStatus {
        val (start, end) = monthRange(now)
        val spent = entries.asSequence().filter {
            it.kind == EntryKind.EXPENSE &&
                (budget.category == "Overall" || it.category == budget.category) &&
                it.createdAt >= start && it.createdAt < end
        }.fold(0L) { total, item -> Math.addExact(total, item.minorUnits) }
        return BudgetStatus(budget.category, budget.limitMinorUnits, spent)
    }

    fun balanceAfterChange(current: Long, kind: SavingsChangeKind, amount: Long): Long {
        require(current >= 0L)
        require(amount > 0L)
        return when (kind) {
            SavingsChangeKind.ADD -> Math.addExact(current, amount)
            SavingsChangeKind.WITHDRAW -> {
                require(amount <= current) { "Cannot withdraw more than recorded savings" }
                Math.subtractExact(current, amount)
            }
        }
    }

    fun progress(saved: Long, target: Long): Float {
        require(saved >= 0L && target > 0L)
        return (saved.toDouble() / target.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
