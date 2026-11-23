package com.example.ui.screens.tenant

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UtilityBillEntity
import com.example.domain.*
import com.example.ui.components.BarComparisonChart
import com.example.ui.components.CategoryBreakdownChart
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot
import com.example.util.PdfExporter

@Composable
fun TenantExpensesScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onOpenSplits: () -> Unit
) {
    val context = LocalContext.current
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val exps = if (group != null) snapshot.expenses.filter { it.groupId == group.id } else emptyList()
    val summary = group?.let { snapshot.financialSummaryForGroup(it.id) }
    val monthBars = exps.groupBy { it.expenseDate.take(7) }
        .entries
        .sortedBy { it.key }
        .map { it.key.takeLast(5) to it.value.sumOf { e -> e.amount }.toInt() }

    Scaffold(
        topBar = {
            RentShareTopBar(
                title = "Expense Dashboard",
                onBack = onBack,
                actions = {
                    if (group != null) {
                        IconButton(
                            onClick = {
                                val file = PdfExporter.exportExpensesCsv(context, group.name, exps, snapshot.usersById)
                                PdfExporter.shareFile(context, file, "text/csv", "Export Expenses CSV")
                                Toast.makeText(context, "CSV exported", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Export expenses CSV")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenSplits,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add / Split") }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (summary != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Current Month", style = MaterialTheme.typography.labelMedium)
                                Text(DomainLogic.formatBdt(summary.currentMonthTotal), style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily), color = IndigoPrimary)
                            }
                        }
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Previous Month", style = MaterialTheme.typography.labelMedium)
                                Text(DomainLogic.formatBdt(summary.previousMonthTotal), style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily), color = TealAccent)
                            }
                        }
                    }
                }
                item { BarComparisonChart("Monthly Shared Spending", monthBars) }
                item { CategoryBreakdownChart("Category Share (Current Month)", summary.categoryTotals) }
            }

            item { Text("Recent Expenses (${exps.size})", style = MaterialTheme.typography.titleMedium) }
            items(exps, key = { it.id }) { exp ->
                val payer = snapshot.usersById[exp.paidByUserId]?.fullName ?: "Member"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, style = MaterialTheme.typography.titleSmall)
                            Text("${exp.category} • ${exp.expenseDate} • Paid by $payer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(DomainLogic.formatBdt(exp.amount), style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily), color = IndigoPrimary)
                            IconButton(onClick = { onDeleteExpense(exp.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete expense", tint = SlateSubtle)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantSplitsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onAddExpense: (
        groupId: Long,
        paidByUserId: Long,
        category: String,
        title: String,
        amount: Double,
        splitType: SplitType,
        participants: List<SplitParticipantInput>,
        notes: String
    ) -> Unit,
    onRecordSettlement: (groupId: Long, fromId: Long, toId: Long, amount: Double, method: String, ref: String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val members = group?.let { snapshot.membersOfGroup(it.id) } ?: emptyList()

    val gExps = if (group != null) snapshot.expenses.filter { it.groupId == group.id } else emptyList()
    val gExpIds = gExps.map { it.id }.toSet()
    val gSplits = snapshot.expenseSplits.filter { it.expenseId in gExpIds }
    val gSettles = if (group != null) snapshot.settlements.filter { it.groupId == group.id } else emptyList()

    val settlementSummary = remember(gExps, gSplits, gSettles) {
        DomainLogic.calculateSettlements(
            expenses = gExps.map { ExpenseLedgerRecord(it.id, it.paidByUserId, it.amount) },
            splits = gSplits.map { SplitLedgerRecord(it.expenseId, it.userId, it.shareAmount) },
            recordedSettlements = gSettles.map { SettlementLedgerRecord(it.fromUserId, it.toUserId, it.amount) }
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var settleTarget by remember { mutableStateOf<TransferEdge?>(null) }

    Scaffold(
        topBar = { RentShareTopBar("Expense Splitting & Settle", onBack = onBack) },
        floatingActionButton = {
            if (group != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New Split") },
                    modifier = Modifier.testTag("add_split_fab")
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (group == null) {
                item { Text("Create or join a Roommate Group first to split expenses.") }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Net Member Balances", style = MaterialTheme.typography.titleMedium)
                            members.forEach { m ->
                                val bal = settlementSummary.netBalances[m.id] ?: 0.0
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(m.fullName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = (if (bal >= 0) "+" else "-") + DomainLogic.formatBdt(kotlin.math.abs(bal)),
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                        color = if (bal >= 0) EmeraldSuccess else RoseError
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.28f)),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Minimized Debt Transfers", style = MaterialTheme.typography.titleMedium)
                            if (settlementSummary.transfers.isEmpty()) {
                                Text("Everyone in ${group.name} is settled up!", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                            } else {
                                settlementSummary.transfers.forEach { edge ->
                                    val fromName = snapshot.usersById[edge.fromUserId]?.fullName ?: "Member"
                                    val toName = snapshot.usersById[edge.toUserId]?.fullName ?: "Member"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("$fromName -> $toName", style = MaterialTheme.typography.titleSmall)
                                            Text(DomainLogic.formatBdt(edge.amount), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily), color = IndigoPrimary)
                                        }
                                        Button(onClick = { settleTarget = edge }, modifier = Modifier.heightIn(min = 48.dp)) {
                                            Text("Record Settle")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Text("Settlement History (${gSettles.size})", style = MaterialTheme.typography.titleMedium) }
                items(gSettles, key = { it.id }) { s ->
                    val from = snapshot.usersById[s.fromUserId]?.fullName ?: "Member"
                    val to = snapshot.usersById[s.toUserId]?.fullName ?: "Member"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("$from paid $to", style = MaterialTheme.typography.titleSmall)
                                Text("${s.paymentMethod} • Ref: ${s.referenceNote.ifBlank { "Manual" }}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(DomainLogic.formatBdt(s.amount), style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily), color = EmeraldSuccess)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && group != null) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Grocery") }
        var splitType by remember { mutableStateOf(SplitType.EQUAL) }
        var notes by remember { mutableStateOf("") }
        val customValues = remember(members) { mutableStateMapOf<Long, String>() }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Shared Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (e.g. DESCO Bill)") })
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Total Amount (৳)") })
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DomainLogic.EXPENSE_CATEGORIES) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SplitType.entries.forEach { st ->
                            FilterChip(
                                selected = splitType == st,
                                onClick = { splitType = st },
                                label = { Text(st.name) }
                            )
                        }
                    }
                    if (splitType != SplitType.EQUAL) {
                        members.forEach { m ->
                            OutlinedTextField(
                                value = customValues[m.id] ?: "",
                                onValueChange = { customValues[m.id] = it },
                                label = { Text("${m.fullName.substringBefore(" ")} (${if (splitType == SplitType.PERCENTAGE) "%" else "৳"})") }
                            )
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val parts = members.map { m ->
                        SplitParticipantInput(m.id, customValues[m.id]?.toDoubleOrNull())
                    }
                    onAddExpense(group.id, uid, category, title, amt, splitType, parts, notes)
                    showAddDialog = false
                }) { Text("Save Split") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    settleTarget?.let { edge ->
        var method by remember { mutableStateOf("bKash / Manual Ref") }
        var ref by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { settleTarget = null },
            title = { Text("Record Manual Settlement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount: ${DomainLogic.formatBdt(edge.amount)}")
                    OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method (Cash / bKash / Nagad)") })
                    OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Transaction Reference") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRecordSettlement(group!!.id, edge.fromUserId, edge.toUserId, edge.amount, method, ref)
                    settleTarget = null
                }) { Text("Confirm Settled") }
            },
            dismissButton = { TextButton(onClick = { settleTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TenantUtilitiesScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onAddBill: (groupId: Long, category: String, name: String, amount: Double, dueDate: String, autoPay: String) -> Unit,
    onMarkPaid: (UtilityBillEntity, String, String) -> Unit,
    onToggleAutoPay: (UtilityBillEntity, String) -> Unit,
    onDeleteBill: (Long) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val bills = if (group != null) snapshot.utilityBills.filter { it.groupId == group.id } else emptyList()

    var showAdd by remember { mutableStateOf(false) }
    var payBill by remember { mutableStateOf<UtilityBillEntity?>(null) }

    Scaffold(
        topBar = { RentShareTopBar("Utility Bills", onBack = onBack) },
        floatingActionButton = {
            if (group != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Bill") }
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bills, key = { it.id }) { bill ->
                val statusInfo = DomainLogic.getUtilityBillStatus(bill.dueDate, bill.paidAt)
                val pillColors = when (statusInfo.status) {
                    "Paid" -> EmeraldSurface to EmeraldSuccess
                    "Overdue" -> RoseSurface to RoseError
                    "Due soon" -> AmberSurface to AmberWarning
                    else -> IndigoLight to IndigoDark
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(bill.billName, style = MaterialTheme.typography.titleMedium)
                                Text("${bill.category} • Due ${bill.dueDate}", style = MaterialTheme.typography.bodySmall)
                            }
                            StatusPill(statusInfo.status, pillColors.first, pillColors.second)
                        }
                        Text(
                            DomainLogic.formatBdt(bill.amount),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = IndigoPrimary
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-pay unavailable: provider integration required", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = false, onCheckedChange = null, enabled = false)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (bill.paidAt.isNullOrBlank()) {
                                Button(onClick = { payBill = bill }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("Record as Paid (Unverified)")
                                }
                            }
                            OutlinedButton(onClick = { onDeleteBill(bill.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd && group != null) {
        var name by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("Electricity") }
        var amt by remember { mutableStateOf("") }
        var due by remember { mutableStateOf("2026-11-28") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Utility Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Bill Name (e.g. Titas Gas)") })
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") })
                    OutlinedTextField(value = amt, onValueChange = { amt = it }, label = { Text("Amount (৳)") })
                    OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("Due Date (YYYY-MM-DD)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddBill(group.id, cat, name, amt.toDoubleOrNull() ?: 0.0, due, "")
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    payBill?.let { bill ->
        var provider by remember { mutableStateOf("bKash (Manual Ref)") }
        var ref by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { payBill = null },
            title = { Text("Record Bill Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record keeping only: this does not send or verify any payment for ${bill.billName} (${DomainLogic.formatBdt(bill.amount)}).")
                    OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Payment Channel") })
                    OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Transaction ID / Receipt Note") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onMarkPaid(bill, provider, ref)
                    payBill = null
                }) { Text("Mark Paid") }
            },
            dismissButton = { TextButton(onClick = { payBill = null }) { Text("Cancel") } }
        )
    }
}
