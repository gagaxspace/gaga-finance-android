package app.gagafinance.android

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
private data class FinanceSnapshot(
    val entries: List<FinanceEntry>,
    val people: List<LedgerPerson>,
    val debts: List<DebtEvent>,
    val budgets: List<MonthlyBudget>,
    val goals: List<SavingsGoal>,
    val savingsChanges: List<SavingsChange>,
    val loans: List<LoanContract>,
    val loanRepayments: List<LoanRepayment>,
    val bills: List<BillReminder>
)

@Composable
private fun GaGaFinanceApp() {
    val context = LocalContext.current
    val store = remember(context) { LocalFinanceStore(context.applicationContext) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<FinanceEntry>>(emptyList()) }
    var people by remember { mutableStateOf<List<LedgerPerson>>(emptyList()) }
    var debtEvents by remember { mutableStateOf<List<DebtEvent>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<MonthlyBudget>>(emptyList()) }
    var goals by remember { mutableStateOf<List<SavingsGoal>>(emptyList()) }
    var savingsChanges by remember { mutableStateOf<List<SavingsChange>>(emptyList()) }
    var loans by remember { mutableStateOf<List<LoanContract>>(emptyList()) }
    var loanRepayments by remember { mutableStateOf<List<LoanRepayment>>(emptyList()) }
    var bills by remember { mutableStateOf<List<BillReminder>>(emptyList()) }
    var hubRoute by rememberSaveable { mutableStateOf("") }
    var editingPerson by remember { mutableStateOf<LedgerPerson?>(null) }
    var showLoanAdd by remember { mutableStateOf(false) }
    var repayingLoan by remember { mutableStateOf<LoanContract?>(null) }
    var showBillAdd by remember { mutableStateOf(false) }
    var pendingBillDelete by remember { mutableStateOf<BillReminder?>(null) }
    var importCandidate by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var resetPhrase by remember { mutableStateOf("") }
    var showProtectExport by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var exportPassphraseConfirm by remember { mutableStateOf("") }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var encryptedImportRaw by remember { mutableStateOf<String?>(null) }
    var importPassphrase by remember { mutableStateOf("") }
    var showEncryptedImport by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(FinanceReminderSettings.isEnabled(context)) }
    var plannerOpen by rememberSaveable { mutableStateOf(false) }
    var budgetDialogOpen by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<MonthlyBudget?>(null) }
    var pendingBudgetDelete by remember { mutableStateOf<MonthlyBudget?>(null) }
    var goalDialogOpen by remember { mutableStateOf(false) }
    var changingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var quickEntryKind by remember { mutableStateOf(EntryKind.EXPENSE) }
    var editingEntry by remember { mutableStateOf<FinanceEntry?>(null) }
    var showPersonAdd by remember { mutableStateOf(false) }
    var recordingPerson by remember { mutableStateOf<LedgerPerson?>(null) }
    var pendingDelete by remember { mutableStateOf<FinanceEntry?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    FinanceSnapshot(
                        entries = store.listEntries(),
                        people = store.listPeople(),
                        debts = store.listDebtEvents(),
                        budgets = store.listBudgets(),
                        goals = store.listSavingsGoals(),
                        savingsChanges = store.listSavingsChanges(),
                        loans = store.listLoans(),
                        loanRepayments = store.listLoanRepayments(),
                        bills = store.listBills()
                    )
                }
                records = snapshot.entries
                people = snapshot.people
                debtEvents = snapshot.debts
                budgets = snapshot.budgets
                goals = snapshot.goals
                savingsChanges = snapshot.savingsChanges
                loans = snapshot.loans
                loanRepayments = snapshot.loanRepayments
                bills = snapshot.bills
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

    val peopleExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val peopleSnapshot = people.toList()
            val eventsSnapshot = debtEvents.toList()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Cannot open selected document")
                        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(PeopleLedgerCsv.export(peopleSnapshot, eventsSnapshot))
                        }
                    }
                    snackbar.showSnackbar("People Ledger exported to the selected location.")
                } catch (_: Exception) {
                    snackbar.showSnackbar("People Ledger export failed.")
                }
            }
        }
    }

    val planningExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val budgetSnapshot = budgets.toList()
            val goalSnapshot = goals.toList()
            val changeSnapshot = savingsChanges.toList()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Cannot open selected document")
                        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(
                                PlanningCsv.export(
                                    budgetSnapshot, goalSnapshot, changeSnapshot
                                )
                            )
                        }
                    }
                    snackbar.showSnackbar("Budget and savings plan CSV exported.")
                } catch (_: Exception) {
                    snackbar.showSnackbar("Planning export failed.")
                }
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val password = pendingExportPassphrase
        pendingExportPassphrase = null
        if (uri != null && password != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val backup = ProtectedBackup.encrypt(store.exportJson(), password)
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: error("Cannot write chosen document")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(backup) }
                    }
                    snackbar.showSnackbar("Encrypted backup saved. Keep your password safe.")
                } catch (_: Exception) {
                    snackbar.showSnackbar("Backup export failed.")
                }
            }
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val candidate = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = stream.read(buffer)
                                if (n < 0) break
                                require(output.size() + n <= 12_000_000) {
                                    "Backup too large"
                                }
                                output.write(buffer, 0, n)
                            }
                            output.toString(Charsets.UTF_8.name())
                        } ?: error("Cannot read chosen file")
                    }
                    if (ProtectedBackup.isProtected(candidate)) {
                        encryptedImportRaw = candidate
                        importPassphrase = ""
                        showEncryptedImport = true
                    } else {
                        // Legacy unencrypted native backups remain importable with confirmation.
                        val preview = withContext(Dispatchers.Default) {
                            store.previewImport(candidate)
                        }
                        importCandidate = candidate
                        importPreview = preview
                    }
                } catch (e: Exception) {
                    snackbar.showSnackbar("Invalid backup: " + (e.message ?: "unsupported file"))
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                FinanceReminderSettings.update(context, true)
                reminderEnabled = true
            } catch (_: Exception) {
                scope.launch { snackbar.showSnackbar("Unable to schedule reminders.") }
            }
        } else {
            reminderEnabled = false
            scope.launch { snackbar.showSnackbar("Notification permission was not granted.") }
        }
    }

    val destinations = listOf(
        Destination("Today", Icons.Filled.Home),
        Destination("Txn", Icons.Filled.ReceiptLong),
        Destination("People", Icons.Filled.People),
        Destination("Insights", Icons.Filled.AutoAwesome),
        Destination("More", Icons.Filled.MoreHoriz)
    )

    MaterialTheme(colorScheme = FinanceTheme) {
        Scaffold(
            containerColor = Navy,
            topBar = { GaGaFinanceBrandBar() },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    destinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = {
                                if (selected != index || index == 4) {
                                    hubRoute = ""
                                    plannerOpen = false
                                }
                                selected = index
                            },
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
                selected == 0 -> DailyHomePage(
                    records = records, budgets = budgets, goals = goals,
                    bills = bills, loans = loans, repayments = loanRepayments,
                    modifier = page,
                    onAddExpense = {
                        editingEntry = null
                        quickEntryKind = EntryKind.EXPENSE
                        showAdd = true
                    },
                    onAddIncome = {
                        editingEntry = null
                        quickEntryKind = EntryKind.INCOME
                        showAdd = true
                    },
                    onTransactions = { selected = 1 },
                    onPeople = { selected = 2 },
                    onBills = {
                        selected = 4
                        plannerOpen = false
                        hubRoute = "obligations"
                    },
                    onPlanning = {
                        selected = 4
                        hubRoute = ""
                        plannerOpen = true
                    }
                )
                selected == 1 -> TransactionsPage(
                    records, page, onAdd = {
                        editingEntry = null
                        quickEntryKind = EntryKind.EXPENSE
                        showAdd = true
                    },
                    onEdit = { editingEntry = it; showAdd = true },
                    onDelete = { pendingDelete = it }
                )
                selected == 2 -> PeopleLedgerPage(
                    people = people,
                    events = debtEvents,
                    modifier = page,
                    onAddPerson = { showPersonAdd = true },
                    onRecord = { recordingPerson = it },
                    onEditPerson = { editingPerson = it }
                )
                selected == 3 -> InsightsPage(
                    entries = records, budgets = budgets,
                    modifier = page,
                    onManageBudgets = { selected = 4; plannerOpen = true }
                )
                selected == 4 && hubRoute == "guide" -> FinanceGuidePage(
                    modifier = page, onBack = { hubRoute = "" }
                )
                selected == 4 && hubRoute == "markets" -> IntegrationStatusPage(
                    type = "markets", modifier = page, onBack = { hubRoute = "" }
                )
                selected == 4 && hubRoute == "banking" -> IntegrationStatusPage(
                    type = "banking", modifier = page, onBack = { hubRoute = "" }
                )
                selected == 4 && hubRoute == "obligations" -> ObligationsPage(
                    people = people, loans = loans, repayments = loanRepayments,
                    bills = bills, modifier = page,
                    onBack = { hubRoute = "" },
                    onAddLoan = { showLoanAdd = true },
                    onRepay = { repayingLoan = it },
                    onAddBill = { showBillAdd = true },
                    onSetBillPaid = { bill, paid ->
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    check(store.setBillPaid(bill.id, paid))
                                }
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Bill update failed.")
                            }
                        }
                    },
                    onDeleteBill = { pendingBillDelete = it }
                )
                selected == 4 && plannerOpen -> PlanningPage(
                    entries = records, budgets = budgets, goals = goals,
                    changes = savingsChanges, modifier = page,
                    onBack = { plannerOpen = false },
                    onBudget = { editingBudget = it; budgetDialogOpen = true },
                    onRemoveBudget = { pendingBudgetDelete = it },
                    onNewGoal = { goalDialogOpen = true },
                    onSavings = { changingGoal = it }
                )
                else -> MorePage(
                    records.size, page,
                    onExport = { exportLauncher.launch("gaga-finance-transactions.csv") },
                    onExportPeople = { peopleExportLauncher.launch("gaga-finance-people-ledger.csv") },
                    onExportPlanning = {
                        planningExportLauncher.launch("gaga-finance-budgets-goals.csv")
                    },
                    onPlanning = { hubRoute = ""; plannerOpen = true },
                    onObligations = { plannerOpen = false; hubRoute = "obligations" },
                    onGuide = { plannerOpen = false; hubRoute = "guide" },
                    onMarkets = { plannerOpen = false; hubRoute = "markets" },
                    onBanking = { plannerOpen = false; hubRoute = "banking" },
                    onExportJson = {
                        exportPassphrase = ""
                        exportPassphraseConfirm = ""
                        showProtectExport = true
                    },
                    reminderEnabled = reminderEnabled,
                    onReminderChange = { enable ->
                        if (!enable) {
                            FinanceReminderSettings.update(context, false)
                            reminderEnabled = false
                        } else if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            try {
                                FinanceReminderSettings.update(context, true)
                                reminderEnabled = true
                            } catch (_: Exception) {
                                scope.launch {
                                    snackbar.showSnackbar("Allow notifications in system Settings.")
                                }
                            }
                        }
                    },
                    onImportJson = { jsonImportLauncher.launch(arrayOf("*/*")) },
                    onResetAll = { resetPhrase = ""; confirmReset = true },
                    overdueBills = bills.count {
                        ObligationMath.billStatus(it, System.currentTimeMillis()) == "Overdue"
                    }
                )
            }
        }

        if (showProtectExport) {
            AlertDialog(
                onDismissRequest = { if (!isSaving) {
                    showProtectExport = false
                    exportPassphrase = ""
                    exportPassphraseConfirm = ""
                } },
                title = { Text("Protect your backup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Choose a unique backup password with at least 12 characters, " +
                            "including letters and numbers. Losing it means losing this backup.")
                        OutlinedTextField(
                            value = exportPassphrase,
                            onValueChange = { exportPassphrase = it.take(128) },
                            label = { Text("Backup password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exportPassphraseConfirm,
                            onValueChange = { exportPassphraseConfirm = it.take(128) },
                            label = { Text("Confirm password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Text("Your password is not stored by the app.",
                            color = Muted, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        enabled = BackupPasswordPolicy.valid(exportPassphrase) &&
                            exportPassphrase == exportPassphraseConfirm && !isSaving,
                        onClick = {
                            pendingExportPassphrase = exportPassphrase
                            exportPassphrase = ""
                            exportPassphraseConfirm = ""
                            showProtectExport = false
                            jsonExportLauncher.launch("gaga-finance-encrypted-backup.json")
                        }
                    ) { Text("Choose backup location") }
                },
                dismissButton = {
                    TextButton(onClick = { showProtectExport = false
                        exportPassphrase = ""
                        exportPassphraseConfirm = ""
                    }) { Text("Cancel") }
                }
            )
        }

        if (showEncryptedImport) {
            AlertDialog(
                onDismissRequest = { if (!isSaving) {
                    showEncryptedImport = false
                    encryptedImportRaw = null
                    importPassphrase = ""
                } },
                title = { Text("Unlock encrypted backup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Enter the password used when this backup was created.")
                        OutlinedTextField(
                            value = importPassphrase,
                            onValueChange = { importPassphrase = it.take(128) },
                            label = { Text("Backup password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(enabled = !isSaving && importPassphrase.isNotEmpty(),
                        onClick = {
                            val file = encryptedImportRaw ?: return@Button
                            val password = importPassphrase
                            importPassphrase = ""
                            isSaving = true
                            scope.launch {
                                try {
                                    val restoredPlaintext = withContext(Dispatchers.Default) {
                                        ProtectedBackup.decrypt(file, password)
                                    }
                                    val preview = withContext(Dispatchers.Default) {
                                        store.previewImport(restoredPlaintext)
                                    }
                                    showEncryptedImport = false
                                    encryptedImportRaw = null
                                    importCandidate = restoredPlaintext
                                    importPreview = preview
                                } catch (_: Exception) {
                                    snackbar.showSnackbar(
                                        "Incorrect password or damaged backup."
                                    )
                                } finally { isSaving = false }
                            }
                        }) { Text(if (isSaving) "Unlocking…" else "Unlock and preview") }
                },
                dismissButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        showEncryptedImport = false
                        encryptedImportRaw = null
                        importPassphrase = ""
                    }) { Text("Cancel") }
                }
            )
        }

        importPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = { if (!isSaving) {
                    importPreview = null
                    importCandidate = null
                } },
                title = { Text("Restore local backup?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "This will replace ALL local records with the chosen JSON backup. " +
                                "It does not sync with the web app. Export your current data first."
                        )
                        Text(preview.total.toString() + " records in backup")
                        Text(
                            preview.recordCounts.entries.joinToString("\n") {
                                it.key + ": " + it.value
                            },
                            color = Muted, fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(enabled = !isSaving, onClick = {
                        val raw = importCandidate ?: return@Button
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { store.restoreJson(raw) }
                                snackbar.showSnackbar("Backup restored successfully.")
                                importPreview = null
                                importCandidate = null
                                reload()
                            } catch (e: Exception) {
                                snackbar.showSnackbar(
                                    "Import rolled back: " + (e.message ?: "validation failed")
                                )
                            } finally { isSaving = false }
                        }
                    }) { Text(if (isSaving) "Restoring…" else "Replace and restore") }
                },
                dismissButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        importPreview = null
                        importCandidate = null
                    }) { Text("Cancel") }
                }
            )
        }

        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { if (!isSaving) confirmReset = false },
                title = { Text("Permanently delete local records?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "All transactions, people, loans, bills, budgets and goals " +
                                "will be erased from this device. This cannot be undone."
                        )
                        Text("Type RESET to confirm.")
                        OutlinedTextField(
                            value = resetPhrase, onValueChange = {
                                resetPhrase = it.take(5)
                            }, singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(enabled = !isSaving && resetPhrase == "RESET", onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { store.clearAllRecords() }
                                confirmReset = false
                                resetPhrase = ""
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not reset local records.")
                            } finally { isSaving = false }
                        }
                    }) { Text("Erase all", color = Coral) }
                },
                dismissButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        confirmReset = false
                    }) { Text("Cancel") }
                }
            )
        }

        editingPerson?.let { person ->
            EditPersonDialog(
                person = person, saving = isSaving,
                onDismiss = { if (!isSaving) editingPerson = null },
                onSave = { changed ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    check(store.updatePerson(changed))
                                }
                                editingPerson = null
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not save contact details.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        if (showLoanAdd) {
            AddLoanDialog(
                people = people, saving = isSaving,
                onDismiss = { if (!isSaving) showLoanAdd = false },
                onSave = { personId, direction, principal, interest, due, note ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.createLoan(personId, direction, principal, interest, due, note)
                                }
                                showLoanAdd = false
                                reload()
                            } catch (e: Exception) {
                                snackbar.showSnackbar(e.message ?: "Could not create loan.")
                            } finally { isSaving = false }
                        }
                    }
                }
            )
        }

        repayingLoan?.let { loan ->
            LoanRepaymentDialog(
                loan = loan, repayments = loanRepayments, saving = isSaving,
                onDismiss = { if (!isSaving) repayingLoan = null },
                onSave = { minor ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.recordLoanRepayment(loan.id, minor)
                                }
                                repayingLoan = null
                                reload()
                            } catch (e: Exception) {
                                snackbar.showSnackbar(e.message ?: "Repayment failed.")
                            } finally { isSaving = false }
                        }
                    }
                }
            )
        }

        if (showBillAdd) {
            AddBillDialog(
                saving = isSaving, onDismiss = { if (!isSaving) showBillAdd = false },
                onSave = { name, amount, due, note ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.createBill(name, amount, due, note)
                                }
                                showBillAdd = false
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not add bill.")
                            } finally { isSaving = false }
                        }
                    }
                }
            )
        }

        pendingBillDelete?.let { bill ->
            AlertDialog(
                onDismissRequest = { if (!isSaving) pendingBillDelete = null },
                title = { Text("Delete bill reminder?") },
                text = { Text("This permanently removes the local bill record.") },
                confirmButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    check(store.deleteBill(bill.id))
                                }
                                pendingBillDelete = null
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not delete bill.")
                            } finally { isSaving = false }
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        pendingBillDelete = null
                    }) { Text("Cancel") }
                }
            )
        }

        if (budgetDialogOpen) {
            BudgetDialog(
                existing = editingBudget, saving = isSaving,
                onDismiss = { if (!isSaving) budgetDialogOpen = false },
                onSave = { category, amount ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.upsertBudget(category, amount)
                                }
                                budgetDialogOpen = false
                                editingBudget = null
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not save the budget.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        pendingBudgetDelete?.let { budget ->
            AlertDialog(
                onDismissRequest = { if (!isSaving) pendingBudgetDelete = null },
                title = { Text("Remove budget?") },
                text = { Text("This removes the spending limit, not any transactions.") },
                confirmButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    check(store.deleteBudget(budget.category))
                                }
                                pendingBudgetDelete = null
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not remove budget.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(enabled = !isSaving, onClick = {
                        pendingBudgetDelete = null
                    }) { Text("Cancel") }
                }
            )
        }

        if (goalDialogOpen) {
            GoalDialog(
                saving = isSaving,
                onDismiss = { if (!isSaving) goalDialogOpen = false },
                onSave = { name, target ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.createSavingsGoal(name, target)
                                }
                                goalDialogOpen = false
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not create savings goal.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        changingGoal?.let { goal ->
            SavingsChangeDialog(
                goal = goal, saving = isSaving,
                onDismiss = { if (!isSaving) changingGoal = null },
                onSave = { kind, amount ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.recordSavingsChange(goal.id, kind, amount)
                                }
                                changingGoal = null
                                reload()
                            } catch (e: IllegalArgumentException) {
                                snackbar.showSnackbar(e.message ?: "Invalid savings amount.")
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not update savings goal.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        if (showAdd) {
            AddTransactionDialog(
                existing = editingEntry,
                defaultKind = quickEntryKind,
                saving = isSaving,
                onDismiss = { if (!isSaving) showAdd = false },
                onSave = { kind, description, minorUnits, category, dateMillis ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val existing = editingEntry
                                    if (existing == null) {
                                        store.addEntry(kind, description, minorUnits, category, dateMillis)
                                    } else {
                                        check(store.updateEntry(existing.id, kind, description, minorUnits, category, dateMillis)) {
                                            "Entry no longer exists"
                                        }
                                    }
                                }
                                showAdd = false
                                editingEntry = null
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

        if (showPersonAdd) {
            AddLedgerPersonDialog(
                saving = isSaving,
                onDismiss = { if (!isSaving) showPersonAdd = false },
                onSave = { name, note ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { store.addPerson(name, note) }
                                showPersonAdd = false
                                reload()
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not save the person.")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        recordingPerson?.let { person ->
            RecordDebtDialog(
                person = person,
                events = debtEvents.filter { it.personId == person.id },
                saving = isSaving,
                onDismiss = { if (!isSaving) recordingPerson = null },
                onSave = { kind, minorUnits, note ->
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    store.addDebtEvent(person.id, kind, minorUnits, note)
                                }
                                recordingPerson = null
                                reload()
                            } catch (e: IllegalArgumentException) {
                                snackbar.showSnackbar(e.message ?: "Invalid ledger amount.")
                            } catch (_: Exception) {
                                snackbar.showSnackbar("Could not record ledger activity.")
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
    records: List<FinanceEntry>,
    budgets: List<MonthlyBudget>,
    goals: List<SavingsGoal>,
    modifier: Modifier,
    onAdd: () -> Unit,
    onView: () -> Unit,
    onPlanning: () -> Unit
) {
    val cashflow = FinanceMath.totals(records, FinanceMath.monthStart())
    Column(
        modifier.background(Navy).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Shared GaGa brand bar appears on every screen.
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
        Text("Plan your money", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    budgets.size.toString() + " monthly budgets · " +
                        goals.size.toString() + " savings goals",
                    color = White, fontWeight = FontWeight.SemiBold
                )
                val overLimit = budgets.count {
                    PlanningMath.budgetStatus(it, records).overBudget
                }
                if (overLimit > 0) {
                    Text(
                        overLimit.toString() + " category budgets exceeded this month",
                        color = Coral, fontSize = 12.sp
                    )
                }
                TextButton(onClick = onPlanning) { Text("Manage budgets & goals") }
            }
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
private fun GaGaFinanceBrandBar() {
    Surface(color = Panel) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.gaga_brand_icon),
                contentDescription = "GaGa Finance logo",
                modifier = Modifier.size(42.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("GaGa Finance", color = White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Text("Smart Finance Hub · Offline", color = Muted, fontSize = 11.sp)
            }
            Text("PREVIEW", color = Aqua,
                fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
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
    onAdd: () -> Unit, onEdit: (FinanceEntry) -> Unit,
    onDelete: (FinanceEntry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("All") }
    var dateFilter by rememberSaveable { mutableStateOf(TransactionFilters.ALL) }
    val visible = remember(records, query, typeFilter, dateFilter) {
        TransactionFilters.apply(records, query, typeFilter, dateFilter)
    }
    LazyColumn(
        modifier.background(Navy),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Shared GaGa brand bar appears on every screen.
                Text("Activity", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Add transaction")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search description or category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Income", "Expense").forEach { value ->
                        FilterChip(
                            selected = typeFilter == value,
                            onClick = { typeFilter = value },
                            label = { Text(value) }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TransactionFilters.ALL, TransactionFilters.MONTH,
                        TransactionFilters.WEEK).forEach { option ->
                        FilterChip(
                            selected = dateFilter == option,
                            onClick = { dateFilter = option },
                            label = { Text(option) }
                        )
                    }
                }
                val filteredTotals = FinanceMath.totals(visible, Long.MIN_VALUE)
                Text("Showing " + visible.size + " of " + records.size + " entries",
                    color = Muted, fontSize = 12.sp)
                Text(
                    "Income " + Money.formatBDT(filteredTotals.income) +
                        "   •   Expenses " + Money.formatBDT(filteredTotals.expenses),
                    color = Aqua, fontSize = 12.sp
                )
            }
        }
        if (visible.isEmpty()) {
            item {
                Text("No matching entries. Try a different search or add a transaction.", color = Muted,
                    modifier = Modifier.padding(vertical = 24.dp))
            }
        } else {
            items(visible, key = { it.id }) { record ->
                EntryCard(record, onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) })
            }
        }
    }
}

@Composable
private fun EntryCard(
    entry: FinanceEntry, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)?
) {
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
                Text(date + " · " + entry.category + " · " +
                    entry.kind.name.lowercase(Locale.getDefault()),
                    color = Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (entry.kind == EntryKind.INCOME) "+ " else "- ") +
                        Money.formatBDT(entry.minorUnits),
                    color = if (entry.kind == EntryKind.INCOME) Aqua else Coral,
                    fontWeight = FontWeight.Bold
                )
                if (onEdit != null || onDelete != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (onEdit != null) {
                            TextButton(onClick = onEdit) { Text("Edit", fontSize = 11.sp) }
                        }
                        if (onDelete != null) {
                            TextButton(onClick = onDelete) { Text("Delete", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    existing: FinanceEntry?, defaultKind: EntryKind,
    saving: Boolean, onDismiss: () -> Unit,
    onSave: (EntryKind, String, Long, String, Long) -> Unit
) {
    var kindName by rememberSaveable(existing?.id, defaultKind) {
        mutableStateOf(existing?.kind?.name ?: defaultKind.name)
    }
    var description by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.description ?: "")
    }
    var amount by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.let { Money.amountForCsv(it.minorUnits) } ?: "")
    }
    var category by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.category ?: "Other")
    }
    var dateText by rememberSaveable(existing?.id) {
        mutableStateOf(
            FinanceDates.formatDate(existing?.createdAt ?: System.currentTimeMillis())
        )
    }
    var menuOpen by remember { mutableStateOf(false) }
    val minorUnits = Money.parseMinorUnits(amount)
    val parsedDate = FinanceDates.parseDate(dateText)
    val dateMillis = if (existing != null &&
        dateText == FinanceDates.formatDate(existing.createdAt)
    ) existing.createdAt else parsedDate
    val validDate = dateMillis != null &&
        dateMillis >= 946684800000L && dateMillis <= System.currentTimeMillis()
    val valid = description.trim().isNotEmpty() && description.trim().length <= 80 &&
        minorUnits != null && validDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New transaction" else "Edit transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        dateText = FinanceDates.formatDate(System.currentTimeMillis())
                    }) { Text("Today") }
                    TextButton(onClick = {
                        dateText = FinanceDates.formatDate(
                            DailyFocusMath.dateRange(
                                DailyFocusMath.dayStart(System.currentTimeMillis()), -1
                            )
                        )
                    }) { Text("Yesterday") }
                }
                OutlinedTextField(
                    value = dateText, onValueChange = { dateText = it.take(10) },
                    label = { Text("Transaction date YYYY-MM-DD") },
                    supportingText = {
                        Text(if (validDate) "Record today or a past date"
                            else "Enter a valid date between 2000-01-01 and today")
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                TextButton(onClick = { menuOpen = true }) { Text("Category: " + category) }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    FinanceCategories.values.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { category = option; menuOpen = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid && !saving,
                onClick = { if (minorUnits != null && dateMillis != null) {
                    onSave(EntryKind.valueOf(kindName), description.trim(),
                        minorUnits, category, dateMillis)
                } }
            ) { Text(if (saving) "Saving…" else if (existing == null) "Save entry" else "Save changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}

@Composable
private fun MorePage(
    count: Int, modifier: Modifier,
    onExport: () -> Unit, onExportPeople: () -> Unit,
    onExportPlanning: () -> Unit, onPlanning: () -> Unit,
    onObligations: () -> Unit, overdueBills: Int,
    onExportJson: () -> Unit, onImportJson: () -> Unit,
    reminderEnabled: Boolean, onReminderChange: (Boolean) -> Unit,
    onResetAll: () -> Unit,
    onGuide: () -> Unit, onMarkets: () -> Unit, onBanking: () -> Unit
) {
    Column(modifier.background(Navy).verticalScroll(rememberScrollState())
        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Shared GaGa brand bar appears on every screen.
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.gaga_brand_icon),
                    contentDescription = "GaGa Finance logo",
                    modifier = Modifier.size(60.dp)
                )
                Column {
                    Text("GaGa Finance", color = White,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Smart Finance Hub", color = Aqua, fontSize = 13.sp)
                    Text("Version 0.7.0 · Offline preview", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Text("Your finance tools", color = White,
            fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(count.toString() + " transactions saved on this device",
            color = Muted, fontSize = 12.sp)
        MoreToolTile("Budgets & savings goals",
            "Monthly limits, goals and progress", onPlanning)
        MoreToolTile(
            "Loans & bill reminders",
            if (overdueBills > 0)
                overdueBills.toString() + " overdue bills · tap to review"
            else "Track repayments, upcoming payments and due dates",
            onObligations
        )
        MoreToolTile("Instructions & safety guide",
            "Learn how to use the app and protect your data", onGuide)

        Text("External services", color = White, fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold)
        MoreToolTile("Markets & portfolio", "Not connected · no live quotes", onMarkets)
        MoreToolTile("Mobile banking",
            "Not connected · no transfers or account balances", onBanking)

        Text("Reminders", color = White, fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Daily due-date notifications", color = White,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional Android reminders for due bills and loan plans. " +
                            "Usually checked around 9 AM, not exact or guaranteed. " +
                            "No personal names on lockscreen.",
                        color = Muted, fontSize = 12.sp
                    )
                }
                Switch(checked = reminderEnabled, onCheckedChange = onReminderChange)
            }
        }

        Text("Backup & data", color = White, fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Protect your records", color = White, fontWeight = FontWeight.Bold)
                Text("Encrypted password-protected JSON backup is recommended. " +
                    "Older unencrypted native backups may still be imported. " +
                    "CSV cannot be restored.",
                    color = Muted, fontSize = 12.sp)
                Button(onClick = onExportJson) { Text("Export encrypted JSON backup") }
                Button(onClick = onImportJson) { Text("Restore JSON backup") }
                Text("Export selected records", color = Muted, fontSize = 12.sp)
                TextButton(onClick = onExport) { Text("Transactions CSV") }
                TextButton(onClick = onExportPeople) { Text("People Ledger CSV") }
                TextButton(onClick = onExportPlanning) { Text("Budgets & goals CSV") }
            }
        }
        Text("CSV exports remain unencrypted. Keep encrypted backup passwords safe.",
            color = Coral, fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Privacy and limitations", color = White, fontWeight = FontWeight.Bold)
                Text("Records stay on this device. No login or cloud sync. Password-encrypted JSON export and confirmed restore are available.",
                    color = Muted)
                Text("Uninstalling or clearing app data can erase records. Export a compatible JSON backup first; CSV files cannot be imported.",
                    color = Muted)
                Text("No bank, wallet, payment service, or cloud AI is connected. Local data is not encrypted at rest.",
                    color = Muted)
            }
        }
        TextButton(onClick = onResetAll) { Text("Reset everything on this device", color = Coral) }
    }
}

@Composable
private fun MoreToolTile(
    title: String, description: String, onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = White, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp)
                Text(description, color = Muted, fontSize = 11.sp)
            }
            TextButton(onClick = onOpen) { Text("Open") }
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
