package app.gagafinance.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0B1424)
private val Panel = Color(0xFF142238)
private val Bright = Color(0xFFF4F7FB)
private val Muted = Color(0xFFB7C5D7)

@Composable
fun FinanceGuidePage(modifier: Modifier, onBack: () -> Unit) {
    val sections = listOf(
        "Transactions" to
            "Add income/expenses in BDT. Select a category. Use Transactions to search, edit and delete.",
        "People Ledger" to
            "Add a person, then record money lent, borrowed and repayments. Open a person to edit contact details or inspect their history. Summary All separates gross and outstanding.",
        "Loans & Borrow" to
            "Create independent dated loan plans, set a simple annual interest estimate and record principal repayments. These contracts are not automatically added to People Ledger totals.",
        "Bills" to
            "Add a bill and due date. Check its Pending, Overdue or Paid status in Loans & Bills. Optional reminders can notify you once daily if allowed, but are not exact. No real payments are made.",
        "Budgets & Goals" to
            "Set category and overall monthly budgets. Track target goals by manually recording saved or reduced amounts.",
        "Spending Insights" to
            "Review a local month-to-date breakdown from entered records. It is not AI advice, market data or a bank reconciliation.",
        "Backup and restore" to
            "More → Export encrypted JSON backup lets you set a password (12+ characters with letters and numbers). Keep it safe; it cannot be recovered. More → Restore JSON can preview and atomically restore the compatible native file. Older plaintext JSON and CSV remain sensitive.",
        "Updating the APK" to
            "Debug APK signing can change between GitHub builds. Export a backup first. Do not uninstall an app that holds important data until you confirm you can recover it.",
        "Security" to
            "Local SQLite records are app-private, but not independently encrypted at rest. Never enter banking passwords, NID/passport numbers, or highly sensitive credentials here. No internet account or banking service is connected.",
        "Privacy policy" to
            "GaGa Finance stores manually entered income/expense records, names/contact details, loans and bill reminders in Android app-private storage only. The offline version has no sign-in, developer-operated data sync, third-party ads or analytics. Optional generic due reminders require user permission. User-initiated exported files may leave app storage. In Settings, confirmed Reset erases local records but not files saved elsewhere. Privacy enquiries: project issue tracker at github.com/gagaxspace/gaga-finance-android/issues (do not post private data). See docs/privacy.html in the public source for the full policy."
    )
    LazyColumn(
        modifier.background(Bg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ More") }
            Text("Instructions", color = Bright, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("How to use GaGa Finance offline", color = Muted)
        }
        sections.forEach { (heading, body) ->
            item(key = heading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(heading, color = Bright, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Text(body, color = Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationStatusPage(
    type: String, modifier: Modifier, onBack: () -> Unit
) {
    val isBanking = type == "banking"
    val title = if (isBanking) "Mobile Banking" else "Markets & Portfolio"
    val details = if (isBanking)
        "No official bank, bKash or Nagad account is connected. The app cannot show verified balances, send money, pay bills or initiate transfers. These actions require a regulated provider partnership, protected authentication and an audited server backend."
    else
        "No live stock, crypto, exchange or investment market feed is connected. Portfolio values are not available. Live quotes and holdings require a trusted data provider and explicit account/data permissions."
    LazyColumn(
        modifier.background(Bg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ More") }
            Text(title, color = Bright, fontSize = 25.sp,
                fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Integration not connected", color = Bright,
                        fontWeight = FontWeight.SemiBold)
                    Text(details, color = Muted)
                    Text(
                        "Use Transactions, People Ledger, Budgets & Goals or Loans & Bills for local manual tracking.",
                        color = Muted, fontSize = 12.sp
                    )
                }
            }
        }
    }
}
