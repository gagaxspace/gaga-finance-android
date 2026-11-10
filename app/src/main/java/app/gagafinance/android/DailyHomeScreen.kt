package app.gagafinance.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HomeBg = Color(0xFF0B1424)
private val HomePanel = Color(0xFF142238)
private val HomeRaised = Color(0xFF1D3049)
private val HomeAqua = Color(0xFF35D6C5)
private val HomeMuted = Color(0xFFC9D6E4)
private val HomeWhite = Color(0xFFF4F7FB)
private val HomeCoral = Color(0xFFFFB4A9)
private val HomeAmber = Color(0xFFFFD38B)

/** Compact daily home: calculated from local entries, not from a bank account. */
@Composable
fun DailyHomePage(
    records: List<FinanceEntry>,
    budgets: List<MonthlyBudget>,
    goals: List<SavingsGoal>,
    bills: List<BillReminder>,
    loans: List<LoanContract>,
    repayments: List<LoanRepayment>,
    modifier: Modifier,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransactions: () -> Unit,
    onPeople: () -> Unit,
    onBills: () -> Unit,
    onPlanning: () -> Unit
) {
    val now = System.currentTimeMillis()
    val focus = DailyFocusMath.calculate(records, budgets, goals, bills, loans, repayments, now)
    val date = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(now))
    Column(
        modifier.background(HomeBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Today", color = HomeWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(date, color = HomeMuted, fontSize = 13.sp)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HomePanel),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("THIS MONTH • NET CASHFLOW", color = HomeMuted, fontSize = 12.sp)
                Text(Money.formatBDT(focus.monthlyNet),
                    color = if (focus.monthlyNet >= 0L) HomeAqua else HomeCoral,
                    fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Income minus expenses from saved entries, not a bank balance",
                    color = HomeMuted, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    SmallStatistic("Income", Money.formatBDT(focus.monthIncome),
                        Modifier.weight(1f), HomeAqua)
                    SmallStatistic("Expenses", Money.formatBDT(focus.monthExpense),
                        Modifier.weight(1f), HomeWhite)
                }
            }
        }
        Text("Quick actions", color = HomeWhite,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddExpense, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("+ Expense", fontSize = 13.sp, maxLines = 1)
            }
            OutlinedButton(onClick = onAddIncome, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("+ Income", fontSize = 13.sp, maxLines = 1)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBills, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                Text("Bills & loans", fontSize = 13.sp)
            }
            OutlinedButton(onClick = onPeople, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                Text("People", fontSize = 13.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SmallStatistic("Today's income", Money.formatBDT(focus.todayIncome),
                Modifier.weight(1f), HomeAqua)
            SmallStatistic("Today's spending", Money.formatBDT(focus.todayExpense),
                Modifier.weight(1f), HomeWhite)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Needs attention", color = HomeWhite,
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (focus.urgentCount > 0) {
                Text(focus.urgentCount.toString() + " urgent",
                    color = HomeCoral, fontSize = 12.sp)
            }
        }
        if (focus.urgentCount == 0 && focus.upcomingBills.isEmpty() &&
            focus.upcomingLoans.isEmpty() && focus.nearBudgets.isEmpty()
        ) {
            FocusCard(
                "All clear", "No due bills, loans or budget warnings in your saved data.",
                HomeAqua, "Review budgets", onPlanning
            )
        } else {
            if (focus.overdueBills.isNotEmpty()) {
                FocusCard(
                    "Overdue bills · " + focus.overdueBills.size,
                    focus.overdueBills.take(2).joinToString(" • ") {
                        it.name + ": " + Money.formatBDT(it.minorUnits)
                    }, HomeCoral, "Review bills", onBills
                )
            }
            if (focus.overdueLoans.isNotEmpty()) {
                FocusCard(
                    "Overdue loan plans · " + focus.overdueLoans.size,
                    "Unsettled principal in manually recorded contracts.",
                    HomeCoral, "Review loans", onBills
                )
            }
            if (focus.overBudgets.isNotEmpty()) {
                FocusCard(
                    "Budgets exceeded · " + focus.overBudgets.size,
                    focus.overBudgets.take(3).joinToString(", ") { it.category },
                    HomeCoral, "Review limits", onPlanning
                )
            }
            if (focus.nearBudgets.isNotEmpty()) {
                FocusCard(
                    "Budget limits approaching",
                    focus.nearBudgets.take(3).joinToString(", ") {
                        it.category + " " + it.percentUsed.toString() + "%"
                    }, HomeAmber, "Review budgets", onPlanning
                )
            }
            if (focus.upcomingBills.isNotEmpty() || focus.upcomingLoans.isNotEmpty()) {
                FocusCard(
                    "Due within 7 days",
                    focus.upcomingBills.size.toString() + " unpaid bills · " +
                        focus.upcomingLoans.size.toString() + " loan plans",
                    HomeAmber, "See due dates", onBills
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Planning & goals", color = HomeWhite,
                fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            TextButton(onClick = onPlanning) { Text("Manage") }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HomePanel)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    budgets.size.toString() + " budget limits · " +
                        goals.size.toString() + " savings goals",
                    color = HomeWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
                Text(focus.goalsReached.toString() + " goals reached",
                    color = HomeAqua, fontSize = 12.sp)
                Text("Goal progress is manual; no funds are held or transferred.",
                    color = HomeMuted, fontSize = 12.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent activity", color = HomeWhite,
                fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            TextButton(onClick = onTransactions) { Text("View all") }
        }
        if (records.isEmpty()) {
            FocusCard("No transactions yet",
                "Record an expense or income to build your personal history.",
                HomeMuted, "Add expense", onAddExpense)
        } else {
            records.take(4).forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HomeRaised)
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.description, color = HomeWhite,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(
                                (if (entry.kind == EntryKind.INCOME) "+ " else "− ") +
                                    Money.formatBDT(entry.minorUnits),
                                color = if (entry.kind == EntryKind.INCOME)
                                    HomeAqua else HomeCoral,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                            )
                        }
                        Text(
                            entry.category + " · " +
                                SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(entry.createdAt)),
                            color = HomeMuted, fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Text(
            "Private offline tracker. Alerts are in-app only; no scheduled system " +
                "notifications or bank feeds are active.",
            color = HomeMuted, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun SmallStatistic(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = HomeRaised),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(title, color = HomeMuted, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(value, color = color, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FocusCard(
    title: String, detail: String, accent: Color,
    actionLabel: String, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HomePanel),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 13.dp, end = 5.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = accent, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, maxLines = 2)
                Text(detail, color = HomeMuted, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onClick) {
                Text(actionLabel, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}
