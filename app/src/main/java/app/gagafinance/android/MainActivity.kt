package app.gagafinance.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF0B1424)
private val Panel = Color(0xFF142238)
private val PanelSoft = Color(0xFF1A2B43)
private val Aqua = Color(0xFF35D6C5)
private val Muted = Color(0xFFB7C5D7)
private val White = Color(0xFFF4F7FB)
private val Coral = Color(0xFFFFB4A9)
private val FinanceTheme = darkColorScheme(
    primary = Aqua, onPrimary = Navy, background = Navy, onBackground = White,
    surface = Panel, onSurface = White, outline = Muted, error = Coral
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GaGaFinanceApp() }
    }
}

private data class Destination(val label: String, val icon: ImageVector)

@Composable
private fun GaGaFinanceApp() {
    val context = LocalContext.current
    val store = remember(context) { LocalFinanceStore(context.applicationContext) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<FinanceEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FinanceEntry?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            try {
                records = withContext(Dispatchers.IO) { store.listEntries() }
                loadError = null
            } catch (_: Exception) {
                loadError = "Unable to read local records. Your existing data has not been erased."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(store) { reload() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val snapshot = records.toList()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Cannot open selected document")
                        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(FinanceCsv.export(snapshot))
                        }
                    }
                    snackbar.showSnackbar("CSV exported to your selected location.")
                } catch (_: Exception) {
                    snackbar.showSnackbar("Export failed. Please choose another location.")
                }
            }
        }
    }

    val destinations = listOf(
        Destination("Home", Icons.Filled.Home),
        Destination("Txn", Icons.Filled.ReceiptLong),
        Destination("People", Icons.Filled.People),
        Destination("AI", Icons.Filled.AutoAwesome),
        Destination("More", Icons.Filled.MoreHoriz)
    )

    MaterialTheme(colorScheme = FinanceTheme) {
        Scaffold(
            containerColor = Navy,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    destinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        ) { padding ->
            val page = Modifier.fillMaxSize().padding(padding)
            when {
                isLoading -> Column(page, verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Aqua)
                }
                loadError != null -> FeatureNotice(
                    "Local data unavailable", loadError ?: "Please retry.", page,
                    action = { Button(onClick = { isLoading = true; reload() }) { Text("Retry") } }
                )
                selected == 0 -> HomePage(
                    records, page, onAdd = { showAdd = true },
                    onView = { selected = 1 }
                )
                selected == 1 -> TransactionsPage(
                    records, page, onAdd = { showAdd = true },
                    onDelete = { pendingDelete = it }
                )
                selected == 2 -> FeatureNotice(
                    "People", "Contacts and transfers are not connected. No money can be sent from this app.",
                    page
                )
                selected == 3 -> FeatureNotice(
                    "AI Advisor", "AI insights are not enabled. No personal transaction data is sent to an AI service.",
                    page
                )
                else -> MorePage(
                    records.size, page,
                    onExport = { exportLauncher.launch("gaga-finance-transactions.csv") }
                )
            }
        }

        if (showAdd) {
            AddTransactionDialog(
                saving = isSaving,
                onDismiss = { if (!isSaving) showAdd = false },
                onSave = { kind, description, minorUnits ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.addEntry(kind, description, minorUnits)
                                }
                                showAdd = false
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not save the transaction.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        pendingDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { if (!isSaving) pendingDelete = null },
                title = { Text("Delete this record?") },
                text = { Text("This removes your local entry permanently. Export a CSV first if needed.") },
                confirmButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { store.deleteEntry(record.id) }
                                pendingDelete = null
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not delete this record.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }) { Text("Delete", color = Coral) }
                },
                dismissButton = {
                    TextButton(enabled = !isSaving, onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomePage(
    records: List<FinanceEntry>, modifier: Modifier,
    onAdd: () -> Unit, onView: () -> Unit
) {
    val cashflow = FinanceMath.totals(records, FinanceMath.monthStart())
    Column(
        modifier.background(Navy).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Header("GaGa Finance", "Private, device-only bookkeeping")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your money overview", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("This month's manually recorded cashflow", color = Muted, fontSize = 13.sp)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("NET CASHFLOW • THIS MONTH", color = Muted, fontSize = 11.sp)
                Text(Money.formatBDT(cashflow.net), color = White,
                    fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("From your own entries · not a bank or wallet balance", color = Muted,
                    fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Add entry")
                    }
                    TextButton(onClick = onView) { Text("View activity") }
                }
            }
        }
        Text("Monthly breakdown", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Income", Money.formatBDT(cashflow.income), Modifier.weight(1f))
            MetricCard("Expenses", Money.formatBDT(cashflow.expenses), Modifier.weight(1f))
        }
        Text("Recent activity", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (records.isEmpty()) {
            Text("Nothing recorded yet. Add an income or expense to get started.", color = Muted)
        } else {
            records.take(3).forEach { EntryCard(it, onDelete = null) }
        }
        Text("Offline tracker only. Payments, deposits and withdrawals are not available.",
            color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, color = Aqua, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Surface(color = PanelSoft, shape = RoundedCornerShape(50)) {
            Text("G", color = Aqua, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun TransactionsPage(
    records: List<FinanceEntry>, modifier: Modifier,
    onAdd: () -> Unit, onDelete: (FinanceEntry) -> Unit
) {
    LazyColumn(
        modifier.background(Navy),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header("Transactions", "Manual entries saved on this device")
                Text("Activity", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Add transaction")
                }
            }
        }
        if (records.isEmpty()) {
            item {
                Text("No entries yet. Start with income or an expense.", color = Muted,
                    modifier = Modifier.padding(vertical = 24.dp))
            }
        } else {
            items(records, key = { it.id }) { record ->
                EntryCard(record, onDelete = { onDelete(record) })
            }
        }
    }
}

@Composable
private fun EntryCard(entry: FinanceEntry, onDelete: (() -> Unit)?) {
    val date = remember(entry.createdAt) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelSoft),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.description, color = White, fontWeight = FontWeight.SemiBold)
                Text(date + " · " + entry.kind.name.lowercase(Locale.getDefault()),
                    color = Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (entry.kind == EntryKind.INCOME) "+ " else "- ") +
                        Money.formatBDT(entry.minorUnits),
                    color = if (entry.kind == EntryKind.INCOME) Aqua else Coral,
                    fontWeight = FontWeight.Bold
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    saving: Boolean, onDismiss: () -> Unit,
    onSave: (EntryKind, String, Long) -> Unit
) {
    var kindName by rememberSaveable { mutableStateOf(EntryKind.EXPENSE.name) }
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    val minorUnits = Money.parseMinorUnits(amount)
    val valid = description.trim().isNotEmpty() && description.trim().length <= 80 &&
        minorUnits != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Manual record only · BDT", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kindName == EntryKind.EXPENSE.name,
                        onClick = { kindName = EntryKind.EXPENSE.name },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = kindName == EntryKind.INCOME.name,
                        onClick = { kindName = EntryKind.INCOME.name },
                        label = { Text("Income") }
                    )
                }
                OutlinedTextField(
                    value = description, onValueChange = { if (it.length <= 80) description = it },
                    label = { Text("Description") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (৳)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Positive amount, up to 2 decimal places") }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid && !saving,
                onClick = { if (minorUnits != null) onSave(EntryKind.valueOf(kindName),
                    description.trim(), minorUnits) }
            ) { Text(if (saving) "Saving…" else "Save entry") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}

@Composable
private fun MorePage(count: Int, modifier: Modifier, onExport: () -> Unit) {
    Column(modifier.background(Navy).verticalScroll(rememberScrollState())
        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Header("More", "Data and account settings")
        Text("Your records", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("You have " + count + " locally recorded transactions.", color = Muted)
        Button(onClick = onExport) { Text("Export transactions to CSV") }
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Privacy and limitations", color = White, fontWeight = FontWeight.Bold)
                Text("Entries are stored privately on this device. There is no login, sync, backup, or account recovery.",
                    color = Muted)
                Text("Removing the app or clearing its data can permanently delete entries. Export a CSV to keep a copy.",
                    color = Muted)
                Text("No bank account, wallet balance, payment service, or AI provider is connected.",
                    color = Muted)
            }
        }
    }
}

@Composable
private fun FeatureNotice(
    title: String, message: String, modifier: Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier.background(Navy).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Aqua)
        Spacer(Modifier.height(18.dp))
        Text(title, color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Muted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}
