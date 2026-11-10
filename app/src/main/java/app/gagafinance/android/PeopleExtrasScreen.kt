package app.gagafinance.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0B1424)
private val Panel = Color(0xFF142238)
private val Aqua = Color(0xFF35D6C5)
private val Muted = Color(0xFFB7C5D7)
private val Bright = Color(0xFFF4F7FB)

@Composable
fun PeopleSummaryPage(
    people: List<LedgerPerson>,
    events: List<DebtEvent>,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val totals = PeopleTotalsMath.calculate(people, events)
    LazyColumn(
        modifier.background(Bg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Back to People") }
            Text("Summary All", color = Bright, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Manual People Ledger · historical totals vs outstanding",
                color = Muted, fontSize = 12.sp)
        }
        item { SummaryStat("TOTAL PEOPLE", totals.people.toString(), "in your ledger") }
        item { SummaryStat("ACTIVE PEOPLE", totals.active.toString(), "with pending balances") }
        item { SummaryStat("TOTAL LENT", Money.formatBDT(totals.totalLent), "all-time recorded") }
        item { SummaryStat("TOTAL BORROWED", Money.formatBDT(totals.totalBorrowed), "all-time recorded") }
        item { SummaryStat("RECEIVED BACK", Money.formatBDT(totals.receivedBack), "repayments received") }
        item { SummaryStat("PAID BACK", Money.formatBDT(totals.paidBack), "repayments paid") }
        item { SummaryStat("OUTSTANDING LENT", Money.formatBDT(totals.lentOutstanding), "still owed to you") }
        item { SummaryStat("OUTSTANDING BORROWED", Money.formatBDT(totals.borrowedOutstanding), "you still owe") }
        item { SummaryStat("NET POSITION", Money.formatBDT(totals.netOutstanding), "lent outstanding less borrowed outstanding") }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Bright, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun EditPersonDialog(
    person: LedgerPerson,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (LedgerPerson) -> Unit
) {
    var name by rememberSaveable(person.id) { mutableStateOf(person.name) }
    var note by rememberSaveable(person.id) { mutableStateOf(person.note) }
    var phone by rememberSaveable(person.id) { mutableStateOf(person.phone) }
    var email by rememberSaveable(person.id) { mutableStateOf(person.email) }
    var address by rememberSaveable(person.id) { mutableStateOf(person.address) }
    var relation by rememberSaveable(person.id) { mutableStateOf(person.relation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit person") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Stored only on this device; not encrypted at rest.",
                    color = Muted, fontSize = 12.sp)
                OutlinedTextField(name, { if (it.length <= 80) name = it },
                    label = { Text("Full name") }, singleLine = true)
                OutlinedTextField(phone, { if (it.length <= 32) phone = it },
                    label = { Text("Phone (optional)") }, singleLine = true)
                OutlinedTextField(email, { if (it.length <= 160) email = it },
                    label = { Text("Email (optional)") }, singleLine = true)
                OutlinedTextField(address, { if (it.length <= 200) address = it },
                    label = { Text("Address (optional)") }, maxLines = 3)
                OutlinedTextField(relation, { if (it.length <= 80) relation = it },
                    label = { Text("Relationship (optional)") }, singleLine = true)
                OutlinedTextField(note, { if (it.length <= 160) note = it },
                    label = { Text("Notes (optional)") }, maxLines = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && name.isNotBlank(),
                onClick = {
                    onSave(
                        person.copy(
                            name = name.trim(), note = note.trim(),
                            phone = phone.trim(), email = email.trim(),
                            address = address.trim(), relation = relation.trim()
                        )
                    )
                }
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}
