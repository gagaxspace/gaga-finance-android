package app.gagafinance.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF0B1424)
private val Panel = Color(0xFF142238)
private val PanelSoft = Color(0xFF1A2B43)
private val Aqua = Color(0xFF35D6C5)
private val Muted = Color(0xFF9AAAC0)
private val White = Color(0xFFF4F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GaGaFinanceApp() }
    }
}

private data class Destination(val label: String, val icon: ImageVector)

@Composable
private fun GaGaFinanceApp() {
    val destinations = listOf(
        Destination("Home", Icons.Filled.Home),
        Destination("Txn", Icons.Filled.ReceiptLong),
        Destination("People", Icons.Filled.People),
        Destination("AI", Icons.Filled.AutoAwesome),
        Destination("More", Icons.Filled.MoreHoriz)
    )
    var selected by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
            Scaffold(
                containerColor = Navy,
                bottomBar = {
                    NavigationBar(containerColor = Panel) {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = { androidx.compose.material3.Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            ) { padding ->
                when (selected) {
                    0 -> Dashboard(Modifier.padding(padding))
                    1 -> PlaceholderPage("Transactions", "Your transaction activity will appear here.")
                    2 -> PlaceholderPage("People", "Manage the people connected to your finance space.")
                    3 -> PlaceholderPage("AI Advisor", "Financial insights will appear here when a trusted data source is connected.")
                    else -> PlaceholderPage("More", "Budgets, savings goals, markets, reminders and settings.")
                }
            }
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(Navy).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("GaGa Finance", color = Aqua, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Smart Finance Hub", color = Muted, fontSize = 12.sp)
            }
            Surface(color = PanelSoft, shape = RoundedCornerShape(50)) {
                Text("G", color = Aqua, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Good day", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Here is your financial overview.", color = Muted, fontSize = 14.sp)
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MONTHLY SAVINGS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("৳ 0.00", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Sample dashboard value · connect your account to load real data", color = Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = {}) { Text("Export statement", color = Aqua) }
                    TextButton(onClick = {}) { Text("Add transaction", color = Aqua) }
                }
            }
        }

        Text("Overview", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Monthly income", "৳ 0.00", Modifier.weight(1f))
            MetricCard("Expenses", "৳ 0.00", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Net savings", "৳ 0.00", Modifier.weight(1f))
            MetricCard("Portfolio", "৳ 0.00", Modifier.weight(1f))
        }
        Spacer(Modifier.height(2.dp))
        Text("Demo shell only — no real money movement is enabled.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaceholderPage(title: String, description: String) {
    Column(
        Modifier.fillMaxSize().background(Navy).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Aqua)
        Spacer(Modifier.height(16.dp))
        Text(title, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Muted)
    }
}
