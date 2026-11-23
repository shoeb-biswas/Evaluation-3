package com.example.ui.screens.public

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.DomainLogic
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot

@Composable
fun PropertyDetailsScreen(
    propertyId: Long,
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onToggleSave: (Long) -> Unit,
    onApplyRental: (moveInDate: String, durationMonths: Int, message: String) -> Unit,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onReportListing: (reason: String, details: String) -> Unit,
    onMessageLandlord: (Long) -> Unit,
    onRequireAuth: () -> Unit
) {
    val property = snapshot.properties.firstOrNull { it.id == propertyId }
    if (property == null) {
        Scaffold(topBar = { RentShareTopBar("Property Details", onBack = onBack) }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Listing not found.")
            }
        }
        return
    }

    val landlord = snapshot.usersById[property.landlordId]
    val landlordTrust = snapshot.trustForUser(property.landlordId)
    val rooms = snapshot.rooms.filter { it.propertyId == property.id }
    val propReviews = snapshot.reviews.filter { it.propertyId == property.id }
    val breakdown = DomainLogic.calculateTotalMonthlyCost(
        property.monthlyRent,
        property.serviceCharge,
        property.estimatedElectricity,
        property.estimatedWater,
        property.estimatedGas,
        property.estimatedInternet,
        property.otherRecurringCharges
    )
    val isSaved = property.id in snapshot.savedPropertyIds
    val currentUser = snapshot.currentUser

    var showApplyDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var ratingInput by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            RentShareTopBar(
                title = property.area,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onToggleSave(property.id) }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save property"
                        )
                    }
                    IconButton(onClick = { showReportDialog = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Flag, contentDescription = "Report listing")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            DomainLogic.formatBdt(property.monthlyRent) + "/mo",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = IndigoPrimary
                        )
                        Text(
                            "Total est. ${DomainLogic.formatBdt(breakdown.totalMonthlyCost)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealAccent
                        )
                    }
                    if (currentUser?.role == "tenant") {
                        OutlinedButton(
                            onClick = { onMessageLandlord(property.landlordId) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { showApplyDialog = true },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("apply_rental_button")
                        ) {
                            Text("Apply Now")
                        }
                    } else if (currentUser == null) {
                        Button(
                            onClick = onRequireAuth,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Sign In to Apply")
                        }
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = MaterialTheme.shapes.large) {
                    PropertyImage(
                        imageKeyOrUri = property.primaryImageKey,
                        contentDescription = property.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }

            if (property.isSuspicious) {
                item {
                    Surface(
                        color = RoseSurface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = RoseError)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Scam Shield Alert", style = MaterialTheme.typography.titleSmall, color = RoseError)
                                Text(property.suspiciousReason, style = MaterialTheme.typography.bodySmall, color = RoseError)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(property.roomType, IndigoLight, IndigoDark)
                        StatusPill(
                            text = if (property.verificationStatus == "approved") "Admin Verified" else property.verificationStatus,
                            containerColor = EmeraldSurface,
                            contentColor = EmeraldSuccess
                        )
                        StatusPill("From ${property.availableFrom}", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(property.title, style = MaterialTheme.typography.headlineSmall)
                    Text(property.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(property.description, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Facilities chips
            item {
                val facilities = property.facilitiesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (facilities.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Facilities & Amenities", style = MaterialTheme.typography.titleMedium)
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                facilities.forEach { fac ->
                                    StatusPill(fac, TealLight.copy(alpha = 0.6f), TealDark, Icons.Filled.Check)
                                }
                            }
                        }
                    }
                }
            }

            // Rooms breakdown
            if (rooms.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Room Inventory (${rooms.count { it.isAvailable }}/${rooms.size} available)", style = MaterialTheme.typography.titleMedium)
                            rooms.forEach { room ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(room.roomLabel, style = MaterialTheme.typography.titleSmall)
                                        Text("${room.roomType} • ${DomainLogic.formatBdt(room.monthlyRent)}/mo", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusPill(
                                        text = if (room.isAvailable) "Available" else "Occupied",
                                        containerColor = if (room.isAvailable) EmeraldSurface else SlateSurfaceVariant,
                                        contentColor = if (room.isAvailable) EmeraldSuccess else SlateSubtle
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { TotalCostBreakdownCard(breakdown, property.advanceAmount) }

            item {
                DhakaLocationMapCanvas(
                    propertyArea = property.area,
                    propertyLat = property.latitude,
                    propertyLng = property.longitude
                )
            }

            item {
                TrustScoreCard(
                    title = "Landlord: ${landlord?.fullName ?: "Owner"}",
                    trust = landlordTrust
                )
            }

            // Reviews
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tenant Reviews (${propReviews.size})", style = MaterialTheme.typography.titleMedium)
                        if (propReviews.isEmpty()) {
                            Text("No reviews yet. Be the first verified tenant to share feedback.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            propReviews.forEach { rev ->
                                val reviewer = snapshot.usersById[rev.reviewerId]?.fullName ?: "Tenant"
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("$reviewer • ${"★".repeat(rev.rating)}", style = MaterialTheme.typography.labelLarge, color = AmberWarning)
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        if (currentUser?.role == "tenant") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Rate:", style = MaterialTheme.typography.labelMedium)
                                (1..5).forEach { star ->
                                    IconButton(onClick = { ratingInput = star }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "$star stars",
                                            tint = if (star <= ratingInput) AmberWarning else SlateBorder
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = reviewText,
                                onValueChange = { reviewText = it },
                                label = { Text("Write a review") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onSubmitReview(ratingInput, reviewText)
                                    reviewText = ""
                                },
                                enabled = reviewText.isNotBlank()
                            ) {
                                Text("Post Review")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApplyDialog) {
        var moveIn by remember { mutableStateOf(property.availableFrom) }
        var months by remember { mutableStateOf("12") }
        var note by remember { mutableStateOf("Hello, I would like to apply for this flat and schedule a visit.") }
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("Apply for Rental") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = moveIn, onValueChange = { moveIn = it }, label = { Text("Preferred Move-in (YYYY-MM-DD)") })
                    OutlinedTextField(value = months, onValueChange = { months = it }, label = { Text("Duration (Months)") })
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Introduction to Landlord") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onApplyRental(moveIn, months.toIntOrNull() ?: 12, note)
                    showApplyDialog = false
                }) {
                    Text("Submit Application")
                }
            },
            dismissButton = { TextButton(onClick = { showApplyDialog = false }) { Text("Cancel") } }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("Misleading rent or deposit") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Suspicious Listing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onReportListing(reason, details)
                    showReportDialog = false
                }) {
                    Text("Submit Report")
                }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}
