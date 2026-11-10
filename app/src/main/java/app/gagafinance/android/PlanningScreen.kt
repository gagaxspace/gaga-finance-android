package app.gagafinance.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ScreenBg = Color(0xFF0B1424)
private val ScreenPanel = Color(0xFF142238)
private val ScreenAqua = Color(0xFF35D6C5)
private val ScreenMuted = Color(0xFFB7C5D7)
private val ScreenText = Color(0xFFF4F7FB)
private val ScreenWarning = Color(0xFFFFB4A9)

@Composable
fun PlanningPage(
    entries: List<FinanceEntry>,
    budgets: List<MonthlyBudget>,
    goals: List<SavingsGoal>,
    changes: List<SavingsChange>,
    modifier: Modifier,
    onBack: () -> Unit,
    onBudget: (MonthlyBudget?) -> Unit,
    onRemoveBudget: (MonthlyBudget) -> Unit,
    onNewGoal: () -> Unit,
    onSavings: (SavingsGoal) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("Budgets") }
    LazyColumn(
        modifier.background(ScreenBg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBack) { Text("‹ More") }
                Text("Budgets & Goals", color = ScreenText,
                    fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Private manual planning. No money is reserved or moved.",
                    color = ScreenMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = tab == "Budgets",
                        onClick = { tab = "Budgets" },
                        label = { Text("Monthly budgets") }
                    )
                    FilterChip(
                        selected = tab == "Goals",
                        onClick = { tab = "Goals" },
                        label = { Text("Savings goals") }
                    )
                }
            }
        }
        if (tab == "Budgets") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) +
                            " · recurring monthly limits",
                        color = ScreenMuted, fontSize = 13.sp
                    )
                    Button(onClick = {
                        onBudget(budgets.firstOrNull { it.category == "Overall" }
                            ?: MonthlyBudget("Overall", 0L))
                    }) { Text("⚙ Set overall budget") }
                    Button(onClick = { onBudget(null) }) { Text("+ Set category budget") }
                    Text("Only recorded expenses count. Limits reset each month.",
                        color = ScreenMuted, fontSize = 12.sp)
                }
            }
            val overall = budgets.firstOrNull { it.category == "Overall" }
            if (overall != null) {
                item {
                    val status = PlanningMath.budgetStatus(overall, entries)
                    Text("Overall monthly progress", color = ScreenText,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    BudgetCard(status, onEdit = { onBudget(overall) },
                        onRemove = { onRemoveBudget(overall) })
                }
            }
            if (budgets.isEmpty()) {
                item { EmptyPlanCard("No budgets yet. Set a monthly spending limit.") }
            } else {
                items(budgets.filter { it.category != "Overall" }, key = { it.category }) { budget ->
                    BudgetCard(
                        PlanningMath.budgetStatus(budget, entries),
                        onEdit = { onBudget(budget) },
                        onRemove = { onRemoveBudget(budget) }
                    )
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onNewGoal) { Text("+ Create savings goal") }
                    Text("Goal progress is entered manually; this is not a wallet balance.",
                        color = ScreenMuted, fontSize = 12.sp)
                }
            }
            if (goals.isEmpty()) {
                item { EmptyPlanCard("No savings goals yet. Add a target to begin.") }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal, changes.filter { it.goalId == goal.id }.take(3),
                        onRecord = { onSavings(goal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ScreenPanel),
        modifier = Modifier.fillMaxWidth()
    ) { Text(message, color = ScreenMuted, modifier = Modifier.padding(18.dp)) }
}

@Composable
private fun BudgetCard(
    status: BudgetStatus, onEdit: () -> Unit, onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenPanel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(status.category, color = ScreenText, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text(
                    if (status.overBudget) "Over budget"
                    else status.percentUsed.toString() + "% used",
                    color = if (status.overBudget) ScreenWarning else ScreenAqua,
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                )
            }
            LinearProgressIndicator(
                progress = { PlanningMath.progress(status.spentMinorUnits, status.limitMinorUnits) },
                modifier = Modifier.fillMaxWidth(),
                color = if (status.overBudget) ScreenWarning else ScreenAqua
            )
            Text(
                "Spent " + Money.formatBDT(status.spentMinorUnits) + " of " +
                    Money.formatBDT(status.limitMinorUnits),
                color = ScreenMuted, fontSize = 12.sp
            )
            Text(
                if (status.overBudget)
                    "Above limit by " +
                        Money.formatBDT(status.spentMinorUnits - status.limitMinorUnits)
                else "Remaining " + Money.formatBDT(status.remainingMinorUnits),
                color = if (status.overBudget) ScreenWarning else ScreenText,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onEdit) { Text("Edit limit") }
                TextButton(onClick = onRemove) { Text("Remove limit") }
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoal, history: List<SavingsChange>, onRecord: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenPanel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(goal.name, color = ScreenText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                Money.formatBDT(goal.savedMinorUnits) + " recorded of " +
                    Money.formatBDT(goal.targetMinorUnits),
                color = ScreenMuted, fontSize = 13.sp
            )
            LinearProgressIndicator(
                progress = {
                    PlanningMath.progress(goal.savedMinorUnits, goal.targetMinorUnits)
                },
                modifier = Modifier.fillMaxWidth(),
                color = ScreenAqua
            )
            Text(
                if (goal.isReached) "Target reached"
                else "Still needed: " + Money.formatBDT(goal.remainingMinorUnits),
                color = if (goal.isReached) ScreenAqua else ScreenText, fontSize = 12.sp
            )
            Button(onClick = onRecord) { Text("Record savings change") }
            if (history.isNotEmpty()) {
                Text("Recent changes", color = ScreenMuted, fontSize = 12.sp)
                history.forEach { change ->
                    Text(
                        (if (change.kind == SavingsChangeKind.ADD) "+ " else "− ") +
                            Money.formatBDT(change.minorUnits) +
                            " · " +
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(Date(change.createdAt)),
                        color = ScreenText, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetDialog(
    existing: MonthlyBudget?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var category by rememberSaveable(existing?.category) {
        mutableStateOf(existing?.category ?: "Food")
    }
    var amount by rememberSaveable(existing?.category) {
        mutableStateOf(existing?.let { Money.amountForCsv(it.limitMinorUnits) } ?: "")
    }
    var expanded by remember { mutableStateOf(false) }
    val parsed = Money.parseMinorUnits(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when {
            existing?.category == "Overall" -> "Overall monthly budget"
            existing == null -> "Set category budget"
            else -> "Edit budget"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Recurring monthly expense limit (BDT).", color = ScreenMuted, fontSize = 12.sp)
                TextButton(onClick = { expanded = true }, enabled = existing == null) {
                    Text("Category: " + category)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    FinanceCategories.values.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { category = option; expanded = false }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Monthly limit (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(enabled = !saving && parsed != null, onClick = {
                if (parsed != null) onSave(category, parsed)
            }) { Text(if (saving) "Saving…" else "Save limit") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GoalDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    val parsed = Money.parseMinorUnits(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A manually tracked target, not an account or money transfer.",
                    color = ScreenMuted, fontSize = 12.sp)
                OutlinedTextField(
                    value = name, onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Goal name") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Target (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(enabled = !saving && name.isNotBlank() && parsed != null, onClick = {
                if (parsed != null) onSave(name.trim(), parsed)
            }) { Text(if (saving) "Saving…" else "Create goal") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SavingsChangeDialog(
    goal: SavingsGoal, saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (SavingsChangeKind, Long) -> Unit
) {
    var kindName by rememberSaveable(goal.id) {
        mutableStateOf(SavingsChangeKind.ADD.name)
    }
    var amount by rememberSaveable(goal.id) { mutableStateOf("") }
    val kind = SavingsChangeKind.valueOf(kindName)
    val parsed = Money.parseMinorUnits(amount)
    val valid = parsed != null &&
        (kind == SavingsChangeKind.ADD || parsed <= goal.savedMinorUnits)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record goal progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(goal.name, color = ScreenText, fontWeight = FontWeight.Bold)
                Text("Already recorded: " + Money.formatBDT(goal.savedMinorUnits),
                    color = ScreenMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == SavingsChangeKind.ADD,
                        onClick = { kindName = SavingsChangeKind.ADD.name },
                        label = { Text("Add savings") }
                    )
                    FilterChip(
                        selected = kind == SavingsChangeKind.WITHDRAW,
                        onClick = { kindName = SavingsChangeKind.WITHDRAW.name },
                        label = { Text("Reduce savings") }
                    )
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                if (parsed != null && !valid) {
                    Text("Cannot reduce more than your recorded progress.",
                        color = ScreenWarning, fontSize = 12.sp)
                }
                Text("No funds are actually moved or reserved.",
                    color = ScreenMuted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(enabled = !saving && valid, onClick = {
                if (parsed != null) onSave(kind, parsed)
            }) { Text(if (saving) "Saving…" else "Record change") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
