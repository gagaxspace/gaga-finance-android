package app.gagafinance.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val InsightNavy = Color(0xFF0B1424)
private val InsightPanel = Color(0xFF142238)
private val InsightAqua = Color(0xFF35D6C5)
private val InsightMuted = Color(0xFFC9D6E4)
private val InsightText = Color(0xFFF4F7FB)
private val InsightWarning = Color(0xFFFFB4A9)

@Composable
fun InsightsPage(
    entries: List<FinanceEntry>,
    budgets: List<MonthlyBudget>,
    modifier: Modifier,
    onManageBudgets: () -> Unit
) {
    val insight = InsightsMath.summarize(entries)
    val overBudgets = budgets.map { PlanningMath.budgetStatus(it, entries) }
        .filter { it.overBudget }
    val maxCategory = insight.categoryExpenses.firstOrNull()?.second ?: 0L
    LazyColumn(
        modifier.background(InsightNavy),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Spending Insights", fontSize = 23.sp,
                    fontWeight = FontWeight.Bold, color = InsightText)
                Text("Private, device-only summaries · not AI advice",
                    color = InsightMuted, fontSize = 12.sp)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = InsightPanel)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) +
                            " (month to date)",
                        color = InsightText, fontWeight = FontWeight.Bold
                    )
                    InsightAmount("Recorded income", insight.currentIncome)
                    InsightAmount("Recorded expenses", insight.currentExpenses)
                    InsightAmount("Net cashflow", insight.currentNet)
                    Text(
                        "Last full calendar month: " +
                            Money.formatBDT(insight.previousMonthExpenses) + " expenses",
                        color = InsightMuted, fontSize = 12.sp
                    )
                    Text(
                        "The current month is incomplete; comparison is descriptive only.",
                        color = InsightMuted, fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Text("Expenses by category", color = InsightText,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        if (insight.categoryExpenses.isEmpty()) {
            item {
                Text(
                    "No expenses recorded this month. Add transactions to see breakdowns.",
                    color = InsightMuted, fontSize = 13.sp
                )
            }
        } else {
            insight.categoryExpenses.forEach { (category, spent) ->
                item(key = "category-" + category) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = InsightPanel)
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category, color = InsightText,
                                    modifier = Modifier.weight(1f))
                                Text(Money.formatBDT(spent), color = InsightAqua)
                            }
                            LinearProgressIndicator(
                                progress = {
                                    if (maxCategory == 0L) 0f else
                                        (spent.toDouble() / maxCategory.toDouble())
                                            .coerceIn(0.0, 1.0).toFloat()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = InsightAqua
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Budget alerts", color = InsightText,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                if (overBudgets.isEmpty()) {
                    Text(
                        if (budgets.isEmpty()) "No limits set yet."
                        else "No recorded categories currently exceed their monthly limits.",
                        color = InsightMuted, fontSize = 12.sp
                    )
                } else {
                    overBudgets.forEach { status ->
                        Text(
                            status.category + ": above limit by " +
                                Money.formatBDT(status.spentMinorUnits - status.limitMinorUnits),
                            color = InsightWarning, fontSize = 13.sp
                        )
                    }
                }
                Button(onClick = onManageBudgets) { Text("Manage budgets & goals") }
                Text(
                    "These insights come only from your manual entries, not verified accounts.",
                    color = InsightMuted, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InsightAmount(label: String, amount: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = InsightMuted, fontSize = 12.sp,
            modifier = Modifier.weight(1f))
        Text(Money.formatBDT(amount), color = InsightText,
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
