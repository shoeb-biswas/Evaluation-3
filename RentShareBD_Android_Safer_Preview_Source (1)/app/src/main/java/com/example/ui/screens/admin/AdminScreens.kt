package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.ComplaintEntity
import com.example.data.local.PropertyEntity
import com.example.data.local.UserEntity
import com.example.data.local.VerificationRecordEntity
import com.example.domain.DomainLogic
import com.example.ui.components.BarComparisonChart
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.navigation.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot
import com.example.util.PdfExporter

@Composable
fun AdminDashboardScreen(
    snapshot: AppSnapshot,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingVerifs = snapshot.verifications.count { it.status == "pending" }
    val suspiciousProps = snapshot.properties.count { it.isSuspicious || it.verificationStatus == "pending" }
    val openComplaints = snapshot.complaints.count { it.status == "open" }
    val avgRentByArea = snapshot.properties.groupBy { it.area }
        .entries
        .map { (area, props) -> area.take(6) to props.map { it.monthlyRent }.average().toInt() }

    val tiles = listOf(
        Triple("User Moderation", "${snapshot.users.size} total accounts", AdminUsersRoute),
        Triple("ID Verifications", "$pendingVerifs pending queue", AdminVerificationsRoute),
        Triple("Scam & Listings", "$suspiciousProps flagged/pending", AdminSuspiciousRoute),
        Triple("Complaints Queue", "$openComplaints open reports", AdminComplaintsRoute),
        Triple("Export Reports", "JSON platform analytics", AdminReportsRoute),
        Triple("Account & Role", "Switch demo workspace", TenantSettingsRoute)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Text("Admin Moderation Hub", style = MaterialTheme.typography.headlineMedium)
                Text("Trust & Safety Operations for Bangladesh", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            BarComparisonChart("Average Monthly Rent by Area", avgRentByArea)
        }
        items(tiles.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (title, sub, route) ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onNavigate(route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AdminUsersScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onToggleSuspend: (UserEntity) -> Unit
) {
    Scaffold(topBar = { RentShareTopBar("Users (${snapshot.users.size})", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(snapshot.users, key = { it.id }) { u ->
                val trust = snapshot.trustForUser(u.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.fullName, style = MaterialTheme.typography.titleSmall)
                            Text("${u.email} • ${u.role.uppercase()} • Trust ${trust.score}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (u.role != "admin") {
                            OutlinedButton(onClick = { onToggleSuspend(u) }) {
                                Text(if (u.isSuspended) "Unsuspend" else "Suspend")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVerificationsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onReview: (VerificationRecordEntity, Boolean) -> Unit
) {
    Scaffold(topBar = { RentShareTopBar("Verification Queue", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(snapshot.verifications, key = { it.id }) { v ->
                val user = snapshot.usersById[v.userId]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user?.fullName ?: "User #${v.userId}", style = MaterialTheme.typography.titleMedium)
                            StatusPill(v.status.uppercase(), if (v.status == "approved") EmeraldSurface else AmberSurface, if (v.status == "approved") EmeraldSuccess else AmberWarning)
                        }
                        Text("Type: ${v.verificationType} • Ref: ${v.documentRef}", style = MaterialTheme.typography.bodySmall)
                        if (v.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onReview(v, true) }, modifier = Modifier.weight(1f)) { Text("Approve") }
                                OutlinedButton(onClick = { onReview(v, false) }, modifier = Modifier.weight(1f)) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSuspiciousScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onReviewProperty: (PropertyEntity, String, Boolean) -> Unit
) {
    val flagged = snapshot.properties.sortedByDescending { it.isSuspicious }
    Scaffold(topBar = { RentShareTopBar("Property & Scam Moderation", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(flagged, key = { it.id }) { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (p.isSuspicious) RoseError.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusPill(if (p.isSuspicious) "SCAM ALERT" else p.verificationStatus, if (p.isSuspicious) RoseSurface else IndigoLight, if (p.isSuspicious) RoseError else IndigoDark)
                        }
                        Text("${p.area} • Rent ${DomainLogic.formatBdt(p.monthlyRent)} • Advance ${DomainLogic.formatBdt(p.advanceAmount)}", style = MaterialTheme.typography.bodySmall)
                        if (p.suspiciousReason.isNotBlank()) {
                            Text("Reason: ${p.suspiciousReason}", style = MaterialTheme.typography.bodySmall, color = RoseError)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onReviewProperty(p, "approved", true) }, modifier = Modifier.weight(1f)) { Text("Approve & Clear") }
                            OutlinedButton(onClick = { onReviewProperty(p, "rejected", false) }, modifier = Modifier.weight(1f)) { Text("Suspend") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminComplaintsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onResolve: (ComplaintEntity, String) -> Unit
) {
    Scaffold(topBar = { RentShareTopBar("Complaints Queue", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(snapshot.complaints, key = { it.id }) { c ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(c.reason, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusPill(c.status.uppercase(), AmberSurface, AmberWarning)
                        }
                        Text(c.details, style = MaterialTheme.typography.bodySmall)
                        if (c.status == "open" || c.status == "investigating") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onResolve(c, "resolved") }, modifier = Modifier.weight(1f)) { Text("Resolve") }
                                OutlinedButton(onClick = { onResolve(c, "dismissed") }, modifier = Modifier.weight(1f)) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReportsScreen(
    snapshot: AppSnapshot,
    reportJson: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(topBar = { RentShareTopBar("Platform Reports", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = {
                        val file = PdfExporter.exportAdminReportJson(context, reportJson)
                        PdfExporter.shareFile(context, file, "application/json", "Export Admin Analytics JSON")
                        Toast.makeText(context, "JSON report exported", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Export JSON Analytics Report")
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = reportJson,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
