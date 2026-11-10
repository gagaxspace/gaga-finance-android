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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

private val Bkg = Color(0xFF0B1424)
private val Panel = Color(0xFF142238)
private val Aqua = Color(0xFF35D6C5)
private val Muted = Color(0xFFB7C5D7)
private val White = Color(0xFFF4F7FB)
private val Coral = Color(0xFFFFB4A9)

@Composable
fun ObligationsPage(
    people: List<LedgerPerson>, loans: List<LoanContract>,
    repayments: List<LoanRepayment>, bills: List<BillReminder>,
    modifier: Modifier, onBack: () -> Unit, onAddLoan: () -> Unit,
    onRepay: (LoanContract) -> Unit, onAddBill: () -> Unit,
    onSetBillPaid: (BillReminder, Boolean) -> Unit,
    onDeleteBill: (BillReminder) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("Loans") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val personNames = people.associate { it.id to it.name }
    val now = System.currentTimeMillis()
    LazyColumn(
        modifier.background(Bkg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("‹ More") }
                Text("Loans & Bills", color = White, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold)
                Text("Manual records only · no payment processing or push reminders",
                    color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tab == "Loans", onClick = { tab = "Loans" },
                        label = { Text("Loans") }
                    )
                    FilterChip(
                        selected = tab == "Bills", onClick = { tab = "Bills" },
                        label = { Text("Bill reminders") }
                    )
                }
            }
        }
        if (tab == "Loans") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddLoan) { Text("+ Add loan") }
                    Text("Loan contracts are tracked separately from People Ledger totals.",
                        color = Muted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Lent", "Borrowed").forEach { option ->
                            FilterChip(
                                selected = filter == option, onClick = { filter = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    SimpleStat(
                        "YOU LENT — UNPAID",
                        Money.formatBDT(
                            ObligationMath.totalOutstanding(loans, repayments, DebtEventKind.LENT)
                        )
                    )
                    SimpleStat(
                        "YOU BORROWED — UNPAID",
                        Money.formatBDT(
                            ObligationMath.totalOutstanding(loans, repayments, DebtEventKind.BORROWED)
                        )
                    )
                    Text(
                        "Overdue: " + loans.count {
                            ObligationMath.status(it, repayments, now) == "Overdue"
                        },
                        color = Coral
                    )
                }
            }
            val visible = loans.filter {
                filter == "All" ||
                    (filter == "Lent" && it.direction == DebtEventKind.LENT) ||
                    (filter == "Borrowed" && it.direction == DebtEventKind.BORROWED)
            }
            if (visible.isEmpty()) {
                item { Text("No matching loan records.", color = Muted) }
            } else {
                items(visible, key = { it.id }) { loan ->
                    val remaining = ObligationMath.loanRemaining(loan, repayments)
                    val status = ObligationMath.status(loan, repayments, now)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(personNames[loan.personId] ?: "Unknown person",
                                color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                (if (loan.direction == DebtEventKind.LENT) "I lent" else "I borrowed") +
                                    " · " + status,
                                color = if (status == "Overdue") Coral else Aqua
                            )
                            Text("Principal: " + Money.formatBDT(loan.principalMinor), color = White)
                            Text("Outstanding principal: " + Money.formatBDT(remaining), color = White)
                            Text("Due date: " + FinanceDates.formatDate(loan.dueAt),
                                color = Muted)
                            Text(
                                "Annual rate: " + (loan.annualInterestBps / 100.0) +
                                    "% · Simple estimated interest: " +
                                    Money.formatBDT(ObligationMath.estimatedInterest(loan)),
                                color = Muted, fontSize = 12.sp
                            )
                            if (loan.note.isNotBlank()) {
                                Text(loan.note, color = Muted, fontSize = 12.sp)
                            }
                            if (remaining > 0L) {
                                Button(onClick = { onRepay(loan) }) {
                                    Text("Record repayment")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddBill) { Text("+ Add bill reminder") }
                    Text(
                        "Overdue: " + bills.count {
                            ObligationMath.billStatus(it, now) == "Overdue"
                        },
                        color = Coral
                    )
                }
            }
            if (bills.isEmpty()) {
                item { Text("No bill reminders yet.", color = Muted) }
            } else {
                items(bills, key = { it.id }) { bill ->
                    val status = ObligationMath.billStatus(bill, now)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(bill.name, color = White, fontSize = 17.sp,
                                fontWeight = FontWeight.Bold)
                            Text(Money.formatBDT(bill.minorUnits), color = White)
                            Text("Due: " + FinanceDates.formatDate(bill.dueAt),
                                color = Muted)
                            Text("Status: " + status,
                                color = if (status == "Overdue") Coral else Aqua)
                            if (bill.note.isNotBlank()) {
                                Text(bill.note, color = Muted, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onSetBillPaid(bill, bill.paidAt == null) }) {
                                    Text(if (bill.paidAt == null) "Mark paid" else "Mark unpaid")
                                }
                                TextButton(onClick = { onDeleteBill(bill) }) {
                                    Text("Delete", color = Coral)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleStat(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = Muted, fontSize = 11.sp)
            Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddLoanDialog(
    people: List<LedgerPerson>, saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, DebtEventKind, Long, Int, Long, String) -> Unit
) {
    var personId by rememberSaveable { mutableStateOf(people.firstOrNull()?.id ?: 0L) }
    var direction by rememberSaveable { mutableStateOf(DebtEventKind.LENT.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("0") }
    var date by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val principal = Money.parseMinorUnits(amount)
    val dueAt = FinanceDates.parseDate(date)
    val rateBps = try {
        BigDecimal(rate.trim()).movePointRight(2).intValueExact()
    } catch (_: Exception) {
        null
    }
    val valid = people.any { it.id == personId } && principal != null &&
        dueAt != null && rateBps != null && rateBps in 0..10000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add loan record") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("A standalone loan plan; not added to People Ledger totals.",
                    color = Muted, fontSize = 12.sp)
                TextButton(onClick = { pickerOpen = true }) {
                    Text("Person: " + (people.firstOrNull { it.id == personId }?.name ?: "Select"))
                }
                DropdownMenu(
                    expanded = pickerOpen, onDismissRequest = { pickerOpen = false }
                ) {
                    people.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name) },
                            onClick = { personId = person.id; pickerOpen = false }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(DebtEventKind.LENT, DebtEventKind.BORROWED).forEach { option ->
                        FilterChip(
                            selected = direction == option.name,
                            onClick = { direction = option.name },
                            label = { Text(option.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Principal (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rate, onValueChange = { rate = it },
                    label = { Text("Estimated annual interest % (0–100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it.take(10) },
                    label = { Text("Due date YYYY-MM-DD") },
                    supportingText = { Text("Example: 2026-12-31") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it.take(160) },
                    label = { Text("Optional note") }, maxLines = 3
                )
                if (!valid) {
                    Text("Select a person and provide a valid amount, rate and date.",
                        color = Muted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(enabled = valid && !saving, onClick = {
                if (principal != null && dueAt != null && rateBps != null) {
                    onSave(personId, DebtEventKind.valueOf(direction), principal,
                        rateBps, dueAt, note)
                }
            }) { Text(if (saving) "Saving…" else "Add loan") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

@Composable
fun LoanRepaymentDialog(
    loan: LoanContract, repayments: List<LoanRepayment>, saving: Boolean,
    onDismiss: () -> Unit, onSave: (Long) -> Unit
) {
    var amount by rememberSaveable(loan.id) { mutableStateOf("") }
    val remaining = ObligationMath.loanRemaining(loan, repayments)
    val parsed = Money.parseMinorUnits(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record repayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Remaining principal: " + Money.formatBDT(remaining))
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Repayment amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("Reduces principal only; does not transfer money or calculate paid interest.",
                    color = Muted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(enabled = parsed != null && parsed <= remaining && !saving,
                onClick = { if (parsed != null) onSave(parsed) }) {
                Text(if (saving) "Saving…" else "Record")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

@Composable
fun AddBillDialog(
    saving: Boolean, onDismiss: () -> Unit,
    onSave: (String, Long, Long, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val parsed = Money.parseMinorUnits(amount)
    val due = FinanceDates.parseDate(date)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add bill reminder") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("In-app due status only. No system push reminder yet.",
                    color = Muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(80) },
                    label = { Text("Bill name") }, singleLine = true
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (৳)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it.take(10) },
                    label = { Text("Due date YYYY-MM-DD") }, singleLine = true
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it.take(160) },
                    label = { Text("Optional note") }, maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && parsed != null && due != null && !saving,
                onClick = {
                    if (parsed != null && due != null) onSave(name.trim(), parsed, due, note)
                }) { Text(if (saving) "Saving…" else "Add bill") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}
