package app.gagafinance.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** User-requested CSV backup of manual planning records and savings history. */
object PlanningCsv {
    fun export(
        budgets: List<MonthlyBudget>,
        goals: List<SavingsGoal>,
        changes: List<SavingsChange>
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return buildString {
            appendLine(
                "record_type,id,name_or_category,target_or_limit_bdt," +
                    "saved_bdt,event_type,event_amount_bdt,event_time_utc"
            )
            budgets.sortedBy { it.category }.forEach { budget ->
                appendLine(
                    "budget,," + quote(budget.category) + "," +
                        Money.amountForCsv(budget.limitMinorUnits) + ",,,,"
                )
            }
            goals.sortedBy { it.id }.forEach { goal ->
                appendLine(
                    "goal," + goal.id + "," + quote(goal.name) + "," +
                        Money.amountForCsv(goal.targetMinorUnits) + "," +
                        Money.amountForCsv(goal.savedMinorUnits) + ",,,"
                )
            }
            val knownGoalIds = goals.map { it.id }.toSet()
            changes.filter { it.goalId in knownGoalIds }
                .sortedWith(compareBy<SavingsChange> { it.createdAt }.thenBy { it.id })
                .forEach { change ->
                    appendLine(
                        "goal_change," + change.goalId + ",,,," +
                            change.kind.name.lowercase(Locale.US) + "," +
                            Money.amountForCsv(change.minorUnits) + "," +
                            timestamp.format(Date(change.createdAt))
                    )
                }
        }
    }

    private fun quote(raw: String): String {
        val clean = raw.replace("\r", " ").replace("\n", " ").trim()
        val safe = if (clean.startsWith("=") || clean.startsWith("+") ||
            clean.startsWith("-") || clean.startsWith("@")) "'" + clean else clean
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
