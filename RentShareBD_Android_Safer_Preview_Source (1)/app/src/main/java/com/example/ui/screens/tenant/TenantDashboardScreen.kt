package com.example.ui.screens.tenant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.DomainLogic
import com.example.domain.ExpenseLedgerRecord
import com.example.domain.SettlementLedgerRecord
import com.example.domain.SplitLedgerRecord
import com.example.ui.components.StatusPill
import com.example.ui.components.TrustScoreCard
import com.example.ui.navigation.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot

private data class WorkspaceShortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: Any,
    val tag: String
)

@Composable
fun TenantDashboardScreen(
    snapshot: AppSnapshot,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val user = snapshot.currentUser ?: return
    val trust = snapshot.trustForUser(user.id)
    val group = snapshot.activeGroupFor(user.id)
    val unreadNotifs = snapshot.unreadNotificationCount()

    val ledger = if (group != null) {
        val gExps = snapshot.expenses.filter { it.groupId == group.id }
        val gExpIds = gExps.map { it.id }.toSet()
        val gSplits = snapshot.expenseSplits.filter { it.expenseId in gExpIds }
        val gSettles = snapshot.settlements.filter { it.groupId == group.id }
        DomainLogic.calculateSettlements(
            expenses = gExps.map { ExpenseLedgerRecord(it.id, it.paidByUserId, it.amount) },
            splits = gSplits.map { SplitLedgerRecord(it.expenseId, it.userId, it.shareAmount) },
            recordedSettlements = gSettles.map { SettlementLedgerRecord(it.fromUserId, it.toUserId, it.amount) }
        )
    } else null

    val myNetBalance = ledger?.netBalances?.get(user.id) ?: 0.0
    val dueBillsCount = if (group != null) {
        snapshot.utilityBills.count { it.groupId == group.id && it.paidAt.isNullOrBlank() }
    } else 0
    val pendingAgreements = snapshot.agreements.count { it.tenantId == user.id && !it.tenantAccepted }

    val shortcuts = listOf(
        WorkspaceShortcut("RentShare AI Chat", "A friendly home companion", Icons.Filled.Forum, TealAccent, AssistantRoute, "tile_ai_chat"),
        WorkspaceShortcut("Wallet Preview", "Real payments not enabled", Icons.Filled.AccountBalanceWallet, AmberWarning, WalletPreviewRoute, "tile_wallet_preview"),
        WorkspaceShortcut("AI Financial Coach", "Gemini MoM insights", Icons.Filled.AutoAwesome, IndigoPrimary, TenantAiCoachRoute, "tile_ai_coach"),
        WorkspaceShortcut("Expenses & Charts", "Analytics & CSV export", Icons.Filled.BarChart, TealAccent, TenantExpensesRoute, "tile_expenses"),
        WorkspaceShortcut("Split & Settle", "Minimize group debts", Icons.Filled.CallSplit, IndigoPrimary, TenantSplitsRoute, "tile_splits"),
        WorkspaceShortcut("Utility Bills", "$dueBillsCount unpaid bills", Icons.Filled.Bolt, AmberWarning, TenantUtilitiesRoute, "tile_utilities"),
        WorkspaceShortcut("Meal Planner", "Sat–Fri menu & voting", Icons.Filled.Restaurant, EmeraldSuccess, TenantMealsRoute, "tile_meals"),
        WorkspaceShortcut("Grocery List", "Sync items to expenses", Icons.Filled.ShoppingCart, TealAccent, TenantGroceriesRoute, "tile_groceries"),
        WorkspaceShortcut("Group Chat", group?.name ?: "Create a group", Icons.Filled.Forum, IndigoPrimary, TenantGroupChatRoute, "tile_group_chat"),
        WorkspaceShortcut("Private Chat", "Message peers & landlords", Icons.Filled.Chat, IndigoDark, TenantPrivateChatRoute(), "tile_private_chat"),
        WorkspaceShortcut("Roommate Group", "Manage flatmates", Icons.Filled.Groups, TealDark, TenantGroupRoute, "tile_group"),
        WorkspaceShortcut("Matches & Requests", "Incoming connections", Icons.Filled.Diversity3, IndigoPrimary, TenantMatchesRoute, "tile_matches"),
        WorkspaceShortcut("Agreements & PDF", "$pendingAgreements pending signature", Icons.Filled.Description, EmeraldSuccess, TenantAgreementsRoute, "tile_agreements"),
        WorkspaceShortcut("Rental Applications", "Track landlord status", Icons.Filled.Assignment, IndigoPrimary, TenantRentalRequestsRoute, "tile_rental_reqs"),
        WorkspaceShortcut("Saved Flats", "${snapshot.savedPropertyIds.size} bookmarked", Icons.Filled.Bookmark, TealAccent, TenantSavedRoute, "tile_saved"),
        WorkspaceShortcut("Maintenance", "Report flat issues", Icons.Filled.Build, AmberWarning, TenantMaintenanceRoute, "tile_maintenance"),
        WorkspaceShortcut("Profile & Verify", "Edit prefs & ID docs", Icons.Filled.Badge, IndigoPrimary, TenantProfileRoute, "tile_profile"),
        WorkspaceShortcut("Account Settings", "Password & demo role", Icons.Filled.Settings, SlateMuted, TenantSettingsRoute, "tile_settings")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tenant Workspace", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${user.fullName} • ${group?.name ?: "No active flat group"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BadgedBox(
                    badge = {
                        if (unreadNotifs > 0) Badge { Text("$unreadNotifs") }
                    }
                ) {
                    FilledTonalIconButton(
                        onClick = { onNavigate(TenantNotificationsRoute) },
                        modifier = Modifier.size(48.dp).testTag("notifications_bell_btn")
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Open notifications")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "My Net Balance",
                    value = (if (myNetBalance >= 0) "+" else "-") + DomainLogic.formatBdt(kotlin.math.abs(myNetBalance)),
                    sub = if (myNetBalance >= 0) "Flatmates owe you" else "You owe group",
                    color = if (myNetBalance >= 0) EmeraldSuccess else RoseError,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending Bills",
                    value = "$dueBillsCount",
                    sub = "Tap Utility Bills",
                    color = if (dueBillsCount > 0) AmberWarning else EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TrustScoreCard(title = "My Trust Score", trust = trust)
        }

        item {
            Text("Shared Living & Rental Tools", style = MaterialTheme.typography.titleLarge)
        }

        items(shortcuts.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { item ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(item.route) }
                            .testTag(item.tag),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(item.icon, contentDescription = null, tint = item.accent, modifier = Modifier.size(24.dp))
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily), color = color)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
