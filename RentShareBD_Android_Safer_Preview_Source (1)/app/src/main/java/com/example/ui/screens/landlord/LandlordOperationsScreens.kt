package com.example.ui.screens.landlord

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.MaintenanceRequestEntity
import com.example.data.local.RentalRequestEntity
import com.example.domain.DomainLogic
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot
import com.example.util.PdfExporter

@Composable
fun LandlordApplicationsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onUpdateApplication: (RentalRequestEntity, String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val myPropIds = snapshot.properties.filter { it.landlordId == uid }.map { it.id }.toSet()
    val apps = snapshot.rentalRequests.filter { it.propertyId in myPropIds }

    Scaffold(topBar = { RentShareTopBar("Rental Applications", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (apps.isEmpty()) item { Text("No applications received yet.") }
            items(apps, key = { it.id }) { req ->
                val prop = snapshot.properties.firstOrNull { it.id == req.propertyId }
                val tenant = snapshot.usersById[req.tenantId]
                val trust = snapshot.trustForUser(req.tenantId)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(tenant?.fullName ?: "Applicant", style = MaterialTheme.typography.titleMedium)
                            StatusPill("Trust ${trust.score}/100", IndigoLight, IndigoDark)
                        }
                        Text(prop?.title ?: "Property #${req.propertyId}", style = MaterialTheme.typography.titleSmall)
                        Text("Move-in: ${req.moveInDate} • ${req.durationMonths} mos", style = MaterialTheme.typography.bodySmall)
                        Text(req.message, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (req.status == "pending") {
                                Button(onClick = { onUpdateApplication(req, "accepted") }, modifier = Modifier.weight(1f)) { Text("Accept & Issue Lease") }
                                OutlinedButton(onClick = { onUpdateApplication(req, "rejected") }, modifier = Modifier.weight(1f)) { Text("Decline") }
                            } else {
                                StatusPill(req.status.uppercase(), EmeraldSurface, EmeraldSuccess)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordTenantsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onMessageTenant: (Long) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val ags = snapshot.agreements.filter { it.landlordId == uid }

    Scaffold(topBar = { RentShareTopBar("Active & Past Tenants", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ags.isEmpty()) item { Text("No tenants under active leases yet.") }
            items(ags, key = { it.id }) { ag ->
                val tenant = snapshot.usersById[ag.tenantId]
                val prop = snapshot.properties.firstOrNull { it.id == ag.propertyId }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tenant?.fullName ?: "Tenant", style = MaterialTheme.typography.titleMedium)
                            Text(prop?.title ?: "Flat #${ag.propertyId}", style = MaterialTheme.typography.bodySmall)
                            Text("${DomainLogic.formatBdt(ag.monthlyRent)}/mo • Status: ${ag.status}", style = MaterialTheme.typography.labelSmall)
                        }
                        FilledTonalIconButton(onClick = { onMessageTenant(ag.tenantId) }) {
                            Icon(Icons.Filled.Chat, contentDescription = "Message tenant")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordAgreementsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uid = snapshot.currentUser?.id ?: return
    val ags = snapshot.agreements.filter { it.landlordId == uid }

    Scaffold(topBar = { RentShareTopBar("Landlord Agreements", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ags, key = { it.id }) { ag ->
                val prop = snapshot.properties.firstOrNull { it.id == ag.propertyId }
                val tenant = snapshot.usersById[ag.tenantId]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Agreement #AG-${ag.id}", style = MaterialTheme.typography.titleMedium)
                            StatusPill(ag.status.uppercase(), EmeraldSurface, EmeraldSuccess)
                        }
                        Text("${prop?.title ?: "Flat"} • Tenant: ${tenant?.fullName ?: "Tenant"}", style = MaterialTheme.typography.bodySmall)
                        Text("${DomainLogic.formatBdt(ag.monthlyRent)}/mo (${ag.startDate} to ${ag.endDate})", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = {
                                val file = PdfExporter.exportAgreementPdf(context, ag, prop, tenant, snapshot.currentUser)
                                PdfExporter.shareFile(context, file, "application/pdf", "Share Rental Agreement")
                                Toast.makeText(context, "Agreement PDF exported", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export Agreement PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordMaintenanceScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onUpdateTicket: (MaintenanceRequestEntity, String, String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val tickets = snapshot.maintenanceRequests.filter { it.landlordId == uid }
    var editing by remember { mutableStateOf<MaintenanceRequestEntity?>(null) }

    Scaffold(topBar = { RentShareTopBar("Maintenance Queue", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (tickets.isEmpty()) item { Text("No maintenance tickets reported.") }
            items(tickets, key = { it.id }) { t ->
                val tenant = snapshot.usersById[t.tenantId]?.fullName ?: "Tenant"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusPill(t.status, AmberSurface, AmberWarning)
                        }
                        Text("From $tenant • ${t.category} (${t.priority})", style = MaterialTheme.typography.bodySmall)
                        Text(t.description, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { editing = t }) { Text("Update Status / Respond") }
                    }
                }
            }
        }
    }

    editing?.let { t ->
        var status by remember { mutableStateOf(t.status) }
        var resp by remember { mutableStateOf(t.landlordResponse) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Respond to Ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Assigned", "In Progress", "Completed").forEach { s ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(value = resp, onValueChange = { resp = it }, label = { Text("Response to Tenant") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTicket(t, status, resp)
                    editing = null
                }) { Text("Save Update") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LandlordReviewsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val reviews = snapshot.reviews.filter { it.landlordId == uid }

    Scaffold(topBar = { RentShareTopBar("Tenant Reviews (${reviews.size})", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (reviews.isEmpty()) item { Text("No tenant reviews yet.") }
            items(reviews, key = { it.id }) { rev ->
                val reviewer = snapshot.usersById[rev.reviewerId]?.fullName ?: "Tenant"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$reviewer • ${"★".repeat(rev.rating)}", style = MaterialTheme.typography.titleSmall, color = AmberWarning)
                        Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
