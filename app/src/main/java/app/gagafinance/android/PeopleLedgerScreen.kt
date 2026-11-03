package app.gagafinance.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LedgerNavy = Color(0xFF0B1424)
private val LedgerPanel = Color(0xFF142238)
private val LedgerSoft = Color(0xFF1A2B43)
private val LedgerAqua = Color(0xFF35D6C5)
private val LedgerMuted = Color(0xFFB7C5D7)
private val LedgerWhite = Color(0xFFF4F7FB)

@Composable
fun PeopleLedgerPage(
    people: List<LedgerPerson>,
    events: List<DebtEvent>,
    modifier: Modifier = Modifier,
    onAddPerson: () -> Unit,
    onRecord: (LedgerPerson) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPersonId by rememberSaveable { mutableStateOf<Long?>(null) }
    val byPerson = remember(events) { events.groupBy { it.personId } }
    val balances = remember(people, events) {
        people.associate { person -> person.id to DebtMath.balance(byPerson[person.id].orEmpty()) }
    }
    val selectedPerson = people.firstOrNull { it.id == selectedPersonId }
    if (selectedPerson != null) {
        PersonHistoryPage(
            selectedPerson, byPerson[selectedPerson.id].orEmpty(),
            balances.getValue(selectedPerson.id), modifier,
            onBack = { selectedPersonId = null },
            onRecord = { onRecord(selectedPerson) }
        )
        return
    }
    val visible = people.filter {
        it.name.contains(query.trim(), ignoreCase = true) ||
            it.note.contains(query.trim(), ignoreCase = true)
    }
    val total = DebtMath.total(balances.values)
    LazyColumn(
        modifier.background(LedgerNavy),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GaGa Finance", color = LedgerAqua, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text("People Ledger", color = LedgerWhite, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold)
                Text("Track money lent and borrowed. Local records only.",
                    color = LedgerMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BalanceBox("OWED TO YOU", total.lentOutstanding, Modifier.weight(1f))
                    BalanceBox("YOU OWE", total.borrowedOutstanding, Modifier.weight(1f))
                }
                Button(onClick = onAddPerson) { Text("+ Add person") }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search people") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(visible.size.toString() + " people shown",
                    color = LedgerMuted, fontSize = 12.sp)
            }
        }
        if (visible.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LedgerPanel)) {
                    Text(
                        if (people.isEmpty()) "No people yet. Add a person to begin."
                        else "No people match your search.",
                        color = LedgerMuted, modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(visible, key = { it.id }) { person ->
                PersonLedgerCard(
                    person, balances.getValue(person.id),
                    onOpen = { selectedPersonId = person.id }
                )
            }
        }
        item {
            Text("Manual ledger only · No bank transfers or contact sync",
                color = LedgerMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BalanceBox(title: String, amount: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerPanel)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 10.sp, color = LedgerMuted, fontWeight = FontWeight.SemiBold)
            Text(Money.formatBDT(amount), fontSize = 17.sp, color = LedgerWhite,
                fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}

@Composable
private fun PersonLedgerCard(
    person: LedgerPerson, balance: DebtBalance, onOpen: () -> Unit
) {
    Card(
        onClick = onOpen, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerPanel)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = LedgerSoft, shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.size(46.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        val initials = person.name.trim().split(Regex("\\s+")).take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
                        Text(initials, color = LedgerAqua, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(person.name, color = LedgerWhite,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (person.note.isNotEmpty()) {
                        Text(person.note, color = LedgerMuted,
                            fontSize = 11.sp, maxLines = 1)
                    }
                }
                Surface(color = LedgerSoft, shape = RoundedCornerShape(25.dp)) {
                    Text(balance.status, color = LedgerAqua, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("LENT / DUE", color = LedgerMuted, fontSize = 10.sp)
                    Text(Money.formatBDT(balance.lentOutstanding), color = LedgerWhite,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("BORROWED / DUE", color = LedgerMuted, fontSize = 10.sp)
                    Text(Money.formatBDT(balance.borrowedOutstanding), color = LedgerWhite,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("View ledger and record repayment", color = LedgerAqua, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PersonHistoryPage(
    person: LedgerPerson, events: List<DebtEvent>, balance: DebtBalance,
    modifier: Modifier, onBack: () -> Unit, onRecord: () -> Unit
) {
    val recent = remember(events) {
        events.sortedWith(compareByDescending<DebtEvent> { it.createdAt }.thenByDescending { it.id })
    }
    LazyColumn(
        modifier.background(LedgerNavy),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text("‹ Back to People") }
                Text(person.name, color = LedgerWhite, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold)
                if (person.note.isNotEmpty()) {
                    Text(person.note, color = LedgerMuted, fontSize = 13.sp)
                }
                Text("Status: " + balance.status, color = LedgerAqua, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BalanceBox("OWED TO YOU", balance.lentOutstanding, Modifier.weight(1f))
                    BalanceBox("YOU OWE", balance.borrowedOutstanding, Modifier.weight(1f))
                }
                Button(onClick = onRecord) { Text("+ Record lending or repayment") }
                Text("Activity history", color = LedgerWhite,
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (recent.isEmpty()) {
            item { Text("No activity recorded for this person.", color = LedgerMuted) }
        } else {
            items(recent, key = { it.id }) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = LedgerPanel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(event.kind.label, color = LedgerWhite,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f))
                            Text(Money.formatBDT(event.minorUnits), color = LedgerAqua,
                                fontWeight = FontWeight.Bold)
                        }
                        Text(
                            SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                                .format(Date(event.createdAt)),
                            color = LedgerMuted, fontSize = 11.sp
                        )
                        if (event.note.isNotBlank()) {
                            Text(event.note, color = LedgerMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item {
            Text("Your own records, not bank-verified statements.",
                color = LedgerMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun AddLedgerPersonDialog(
    saving: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add person") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 160) note = it },
                    label = { Text("Nickname or note (optional)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
                Text("Not synced to contacts or cloud.", color = LedgerMuted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && name.isNotBlank(),
                onClick = { onSave(name.trim(), note.trim()) }
            ) { Text(if (saving) "Saving…" else "Add") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordDebtDialog(
    person: LedgerPerson, events: List<DebtEvent>, saving: Boolean,
    onDismiss: () -> Unit, onSave: (DebtEventKind, Long, String) -> Unit
) {
    val balance = remember(events) { DebtMath.balance(events) }
    var kindName by rememberSaveable(person.id) { mutableStateOf(DebtEventKind.LENT.name) }
    var amount by rememberSaveable(person.id) { mutableStateOf("") }
    var note by rememberSaveable(person.id) { mutableStateOf("") }
    val kind = DebtEventKind.valueOf(kindName)
    val minorUnits = Money.parseMinorUnits(amount)
    val valid = minorUnits != null && DebtMath.canAdd(balance, kind, minorUnits)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record: " + person.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Manual record only — no money is transferred.",
                    color = LedgerMuted, fontSize = 12.sp)
                Text("Owed to you: " + Money.formatBDT(balance.lentOutstanding),
                    color = LedgerWhite, fontSize = 13.sp)
                Text("You owe: " + Money.formatBDT(balance.borrowedOutstanding),
                    color = LedgerWhite, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DebtEventKind.LENT, DebtEventKind.BORROWED).forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kindName = option.name },
                            label = { Text(option.label, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DebtEventKind.REPAID_TO_ME, DebtEventKind.I_REPAID).forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kindName = option.name },
                            label = { Text(option.label, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (৳)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (minorUnits != null && !valid) {
                    Text("Repayment exceeds the outstanding amount.",
                        color = LedgerMuted, fontSize = 11.sp)
                }
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 160) note = it },
                    label = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && valid,
                onClick = {
                    if (minorUnits != null) onSave(kind, minorUnits, note.trim())
                }
            ) { Text(if (saving) "Saving…" else "Record") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
