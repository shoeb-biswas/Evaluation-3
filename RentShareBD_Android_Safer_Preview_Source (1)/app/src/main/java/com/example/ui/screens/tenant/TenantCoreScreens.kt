package com.example.ui.screens.tenant

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.RentalAgreementEntity
import com.example.data.local.RoommatePreferenceEntity
import com.example.data.local.RoommateRequestEntity
import com.example.data.local.TenantProfileEntity
import com.example.BuildConfig
import com.example.domain.DomainLogic
import com.example.ui.components.PropertyCard
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.TrustScoreCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot
import com.example.util.PdfExporter

@Composable
fun TenantProfileScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onSaveProfile: (com.example.data.local.UserEntity, TenantProfileEntity, RoommatePreferenceEntity) -> Unit,
    onVerifyEmail: (Long) -> Unit,
    onSubmitVerification: (Long, String, String) -> Unit
) {
    val user = snapshot.currentUser ?: return
    val existingTp = snapshot.tenantProfiles.firstOrNull { it.userId == user.id } ?: TenantProfileEntity(userId = user.id)
    val existingPref = snapshot.preferences.firstOrNull { it.userId == user.id } ?: RoommatePreferenceEntity(userId = user.id)

    var fullName by remember(user) { mutableStateOf(user.fullName) }
    var phone by remember(user) { mutableStateOf(user.phone) }
    var occupation by remember(existingTp) { mutableStateOf(existingTp.occupation) }
    var workplace by remember(existingTp) { mutableStateOf(existingTp.institutionOrWorkplace) }
    var area by remember(existingPref) { mutableStateOf(existingPref.preferredLocation) }
    var budget by remember(existingPref) { mutableStateOf(existingPref.monthlyBudget.toString()) }
    var bio by remember(existingTp) { mutableStateOf(existingTp.bio) }
    var smoking by remember(existingPref) { mutableStateOf(existingPref.smokingPreference) }
    var sleep by remember(existingPref) { mutableStateOf(existingPref.sleepSchedule) }
    var noise by remember(existingPref) { mutableStateOf(existingPref.noisePreference) }
    var cleanliness by remember(existingPref) { mutableFloatStateOf(existingPref.cleanlinessLevel.toFloat()) }
    var docRef by remember { mutableStateOf("") }

    Scaffold(topBar = { RentShareTopBar("Profile & Verification", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { TrustScoreCard("Identity & Trust Score", snapshot.trustForUser(user.id)) }

            if (!user.emailVerified) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = AmberSurface)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Email verification is not yet connected", style = MaterialTheme.typography.labelLarge, color = AmberWarning)
                            Button(onClick = { }, enabled = false) { Text("Unavailable") }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Personal & Lifestyle Preferences", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = workplace, onValueChange = { workplace = it }, label = { Text("University / Workplace") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Preferred Area (e.g. Mirpur)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Monthly Budget (BDT)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Short Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                        Text("Smoking", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("non_smoker" to "Non-smoker", "outside_only" to "Outside", "smoker" to "Smoker").forEach { (k, l) ->
                                FilterChip(selected = smoking == k, onClick = { smoking = k }, label = { Text(l) })
                            }
                        }

                        Text("Sleep & Noise", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("early_bird" to "Early", "moderate" to "Moderate", "night_owl" to "Night Owl").forEach { (k, l) ->
                                FilterChip(selected = sleep == k, onClick = { sleep = k }, label = { Text(l) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("quiet" to "Quiet", "moderate" to "Moderate", "lively" to "Lively").forEach { (k, l) ->
                                FilterChip(selected = noise == k, onClick = { noise = k }, label = { Text(l) })
                            }
                        }

                        Text("Cleanliness Level: ${cleanliness.toInt()}/5", style = MaterialTheme.typography.labelLarge)
                        Slider(value = cleanliness, onValueChange = { cleanliness = it }, valueRange = 1f..5f, steps = 3)

                        Button(
                            onClick = {
                                val bVal = budget.toIntOrNull() ?: 9000
                                onSaveProfile(
                                    user.copy(fullName = fullName, phone = phone),
                                    existingTp.copy(occupation = occupation, institutionOrWorkplace = workplace, preferredLocation = area, monthlyBudget = bVal, bio = bio),
                                    existingPref.copy(preferredLocation = area, monthlyBudget = bVal, smokingPreference = smoking, sleepSchedule = sleep, noisePreference = noise, cleanlinessLevel = cleanliness.toInt())
                                )
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_profile_btn")
                        ) {
                            Text("Save Profile & Preferences")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Submit Student ID / NID Verification", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = docRef,
                            onValueChange = { docRef = it },
                            label = { Text("Example reference (never real NID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSubmitVerification(user.id, "student_id", docRef)
                                docRef = ""
                            },
                            enabled = docRef.isNotBlank()
                        ) {
                            Text("Submit for Admin Approval")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantMatchesScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onRespondRequest: (RoommateRequestEntity, String) -> Unit,
    onMessagePeer: (Long) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val myReqs = snapshot.roommateRequests.filter { it.senderId == uid || it.receiverId == uid }

    Scaffold(topBar = { RentShareTopBar("Matches & Connections", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (myReqs.isEmpty()) {
                item { Text("No roommate requests yet. Discover flatmates in the Roommates tab.") }
            }
            items(myReqs, key = { it.id }) { req ->
                val isIncoming = req.receiverId == uid
                val otherId = if (isIncoming) req.senderId else req.receiverId
                val other = snapshot.usersById[otherId]
                val compat = snapshot.compatibilityBetween(uid, otherId)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${other?.fullName ?: "Member"} (${compat.score}% match)", style = MaterialTheme.typography.titleMedium)
                            StatusPill(
                                text = req.status.uppercase(),
                                containerColor = if (req.status == "accepted") EmeraldSurface else IndigoLight,
                                contentColor = if (req.status == "accepted") EmeraldSuccess else IndigoDark
                            )
                        }
                        Text(req.message, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isIncoming && req.status == "pending") {
                                Button(onClick = { onRespondRequest(req, "accepted") }, modifier = Modifier.weight(1f)) { Text("Accept") }
                                OutlinedButton(onClick = { onRespondRequest(req, "rejected") }, modifier = Modifier.weight(1f)) { Text("Decline") }
                            } else if (req.status == "accepted") {
                                OutlinedButton(onClick = { onMessagePeer(otherId) }, modifier = Modifier.fillMaxWidth()) { Text("Open Private Chat") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantRentalRequestsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onOpenProperty: (Long) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val myReqs = snapshot.rentalRequests.filter { it.tenantId == uid }

    Scaffold(topBar = { RentShareTopBar("My Rental Applications", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (myReqs.isEmpty()) {
                item { Text("You haven't submitted any rental applications yet.") }
            }
            items(myReqs, key = { it.id }) { req ->
                val prop = snapshot.properties.firstOrNull { it.id == req.propertyId }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prop?.title ?: "Property #${req.propertyId}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusPill(
                                text = req.status.uppercase(),
                                containerColor = if (req.status == "accepted") EmeraldSurface else AmberSurface,
                                contentColor = if (req.status == "accepted") EmeraldSuccess else AmberWarning
                            )
                        }
                        Text("Move-in: ${req.moveInDate} • ${req.durationMonths} months", style = MaterialTheme.typography.bodySmall)
                        Text(req.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (prop != null) {
                            TextButton(onClick = { onOpenProperty(prop.id) }) { Text("View Listing") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantSavedScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onOpenProperty: (Long) -> Unit,
    onToggleSave: (Long) -> Unit
) {
    val saved = snapshot.properties.filter { it.id in snapshot.savedPropertyIds }
    Scaffold(topBar = { RentShareTopBar("Saved Properties (${saved.size})", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (saved.isEmpty()) {
                item { Text("No bookmarked flats yet. Tap the bookmark icon on any listing to save it.") }
            }
            items(saved, key = { it.id }) { prop ->
                PropertyCard(
                    property = prop,
                    landlordVerified = snapshot.usersById[prop.landlordId]?.identityVerified == true,
                    isSaved = true,
                    onToggleSave = { onToggleSave(prop.id) },
                    onClick = { onOpenProperty(prop.id) }
                )
            }
        }
    }
}

@Composable
fun TenantAgreementsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onAcceptAgreement: (RentalAgreementEntity) -> Unit
) {
    val context = LocalContext.current
    val uid = snapshot.currentUser?.id ?: return
    val myAgs = snapshot.agreements.filter { it.tenantId == uid }

    Scaffold(topBar = { RentShareTopBar("Digital Rental Agreements", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (myAgs.isEmpty()) {
                item { Text("No rental agreements yet. Once a landlord accepts your application, an agreement appears here.") }
            }
            items(myAgs, key = { it.id }) { ag ->
                val prop = snapshot.properties.firstOrNull { it.id == ag.propertyId }
                val landlord = snapshot.usersById[ag.landlordId]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Agreement #AG-${ag.id}", style = MaterialTheme.typography.titleMedium)
                            StatusPill(
                                text = ag.status.uppercase(),
                                containerColor = if (ag.status == "active") EmeraldSurface else AmberSurface,
                                contentColor = if (ag.status == "active") EmeraldSuccess else AmberWarning
                            )
                        }
                        Text(prop?.title ?: "Property #${ag.propertyId}", style = MaterialTheme.typography.titleSmall)
                        Text("Landlord: ${landlord?.fullName ?: "Owner"}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Rent: ${DomainLogic.formatBdt(ag.monthlyRent)}/mo • Advance: ${DomainLogic.formatBdt(ag.advanceAmount)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
                        )
                        Text("Term: ${ag.startDate} to ${ag.endDate} (${ag.noticePeriodDays}d notice)", style = MaterialTheme.typography.bodySmall)
                        Text("Utilities: ${ag.utilityTerms}", style = MaterialTheme.typography.bodySmall)
                        Text("House Rules: ${ag.houseRules}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!ag.tenantAccepted) {
                                Button(
                                    onClick = { onAcceptAgreement(ag) },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text("Accept & Sign")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    val file = PdfExporter.exportAgreementPdf(context, ag, prop, snapshot.currentUser, landlord)
                                    PdfExporter.shareFile(context, file, "application/pdf", "Share Rental Agreement PDF")
                                    Toast.makeText(context, "Agreement PDF ready", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_pdf_${ag.id}")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}
