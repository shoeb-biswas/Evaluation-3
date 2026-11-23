package com.example.ui.screens.landlord

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.local.RoomEntity
import com.example.domain.DomainLogic
import com.example.ui.components.PropertyImage
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.TrustScoreCard
import com.example.ui.navigation.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot

@Composable
fun LandlordDashboardScreen(
    snapshot: AppSnapshot,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val user = snapshot.currentUser ?: return
    val myProps = snapshot.properties.filter { it.landlordId == user.id }
    val myPropIds = myProps.map { it.id }.toSet()
    val pendingApps = snapshot.rentalRequests.count { it.propertyId in myPropIds && it.status == "pending" }
    val activeAgs = snapshot.agreements.count { it.landlordId == user.id && it.status == "active" }
    val openMaint = snapshot.maintenanceRequests.count { it.landlordId == user.id && it.status != "Completed" }

    val shortcuts = listOf(
        Triple("My Properties", "${myProps.size} listings", LandlordPropertiesRoute),
        Triple("Add New Flat", "Publish verified room", LandlordPropertyEditorRoute(null)),
        Triple("Applications", "$pendingApps pending review", LandlordApplicationsRoute),
        Triple("Active Tenants", "Roster & direct chat", LandlordTenantsRoute),
        Triple("Agreements & PDF", "$activeAgs active leases", LandlordAgreementsRoute),
        Triple("Maintenance", "$openMaint open tickets", LandlordMaintenanceRoute),
        Triple("Tenant Reviews", "Ratings & trust feedback", LandlordReviewsRoute),
        Triple("Account & Role", "Password & demo switch", TenantSettingsRoute)
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
                Text("Landlord Portal", style = MaterialTheme.typography.headlineMedium)
                Text(user.fullName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiBox("Listings", "${myProps.size}", IndigoPrimary, Modifier.weight(1f))
                KpiBox("Pending Apps", "$pendingApps", AmberWarning, Modifier.weight(1f))
                KpiBox("Open Tickets", "$openMaint", TealAccent, Modifier.weight(1f))
            }
        }
        item { TrustScoreCard("Landlord Trust Score", snapshot.trustForUser(user.id)) }
        item { Text("Management Workflows", style = MaterialTheme.typography.titleLarge) }
        items(shortcuts.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (title, sub, route) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(route) },
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
private fun KpiBox(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFamily), color = color)
        }
    }
}

@Composable
fun LandlordPropertiesScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onAddProperty: () -> Unit,
    onEditProperty: (Long) -> Unit,
    onToggleRoom: (RoomEntity) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val props = snapshot.properties.filter { it.landlordId == uid }

    Scaffold(
        topBar = { RentShareTopBar("My Properties (${props.size})", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProperty,
                icon = { Icon(Icons.Filled.AddHome, contentDescription = null) },
                text = { Text("Add Property") }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(props, key = { it.id }) { prop ->
                val rooms = snapshot.rooms.filter { it.propertyId == prop.id }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PropertyImage(prop.primaryImageKey, prop.title, Modifier.size(72.dp))
                            Column(Modifier.weight(1f)) {
                                Text(prop.title, style = MaterialTheme.typography.titleSmall)
                                Text("${prop.area} • ${DomainLogic.formatBdt(prop.monthlyRent)}/mo", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(prop.verificationStatus, IndigoLight, IndigoDark)
                                    if (prop.isSuspicious) StatusPill("Flagged", RoseSurface, RoseError)
                                }
                            }
                            IconButton(onClick = { onEditProperty(prop.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit property")
                            }
                        }
                        if (rooms.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Text("Room Availability Toggles", style = MaterialTheme.typography.labelMedium)
                            rooms.forEach { room ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${room.roomLabel} (${DomainLogic.formatBdt(room.monthlyRent)})", style = MaterialTheme.typography.bodySmall)
                                    Switch(checked = room.isAvailable, onCheckedChange = { onToggleRoom(room) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordPropertyEditorScreen(
    propertyId: Long?,
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onSave: (
        existingId: Long?,
        title: String,
        description: String,
        area: String,
        address: String,
        roomType: String,
        totalRooms: Int,
        monthlyRent: Int,
        advanceAmount: Int,
        serviceCharge: Int,
        electricity: Int,
        water: Int,
        gas: Int,
        internet: Int,
        otherCharges: Int,
        availableFrom: String,
        furnished: Boolean,
        attachedBath: Boolean,
        hasBalcony: Boolean,
        hasWifi: Boolean,
        hasParking: Boolean,
        hasGenerator: Boolean,
        imageKeyOrUri: String,
        facilitiesCsv: String
    ) -> Unit
) {
    val context = LocalContext.current
    val existing = propertyId?.let { id -> snapshot.properties.firstOrNull { it.id == id } }

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var desc by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var area by remember(existing) { mutableStateOf(existing?.area ?: "Mirpur") }
    var address by remember(existing) { mutableStateOf(existing?.address ?: "") }
    var roomType by remember(existing) { mutableStateOf(existing?.roomType ?: "Single Room") }
    var roomsStr by remember(existing) { mutableStateOf((existing?.totalRooms ?: 3).toString()) }
    var rentStr by remember(existing) { mutableStateOf((existing?.monthlyRent ?: 8500).toString()) }
    var advStr by remember(existing) { mutableStateOf((existing?.advanceAmount ?: 17000).toString()) }
    var serviceStr by remember(existing) { mutableStateOf((existing?.serviceCharge ?: 500).toString()) }
    var elecStr by remember(existing) { mutableStateOf((existing?.estimatedElectricity ?: 1000).toString()) }
    var waterStr by remember(existing) { mutableStateOf((existing?.estimatedWater ?: 200).toString()) }
    var gasStr by remember(existing) { mutableStateOf((existing?.estimatedGas ?: 300).toString()) }
    var netStr by remember(existing) { mutableStateOf((existing?.estimatedInternet ?: 500).toString()) }
    var availFrom by remember(existing) { mutableStateOf(existing?.availableFrom ?: "2026-12-01") }
    var furnished by remember(existing) { mutableStateOf(existing?.furnished ?: true) }
    var hasWifi by remember(existing) { mutableStateOf(existing?.hasWifi ?: true) }
    var hasGenerator by remember(existing) { mutableStateOf(existing?.hasGenerator ?: true) }
    var imageKeyOrUri by remember(existing) { mutableStateOf(existing?.primaryImageKey ?: "img_prop_mirpur") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imageKeyOrUri = uri.toString()
        }
    }

    Scaffold(topBar = { RentShareTopBar(if (existing == null) "Add New Flat" else "Edit Flat", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PropertyImage(imageKeyOrUri, "Property preview", Modifier.fillMaxWidth().height(150.dp))
                        OutlinedButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Choose Photo (Android Photo Picker)")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Listing Title") }, modifier = Modifier.fillMaxWidth().testTag("editor_title"))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DomainLogic.DHAKA_AREAS) { a ->
                        FilterChip(selected = area == a, onClick = { area = a }, label = { Text(a) })
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Single Room", "Shared Room", "Master Suite").forEach { rt ->
                        FilterChip(selected = roomType == rt, onClick = { roomType = rt }, label = { Text(rt) })
                    }
                }
            }
            item { OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Full Address") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rentStr, onValueChange = { rentStr = it }, label = { Text("Monthly Rent (৳)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = advStr, onValueChange = { advStr = it }, label = { Text("Advance (৳)") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = serviceStr, onValueChange = { serviceStr = it }, label = { Text("Service (৳)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = elecStr, onValueChange = { elecStr = it }, label = { Text("Elec (৳)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = gasStr, onValueChange = { gasStr = it }, label = { Text("Gas (৳)") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = waterStr, onValueChange = { waterStr = it }, label = { Text("Water (৳)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = netStr, onValueChange = { netStr = it }, label = { Text("Wi-Fi (৳)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = roomsStr, onValueChange = { roomsStr = it }, label = { Text("Rooms") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = furnished, onClick = { furnished = !furnished }, label = { Text("Furnished") })
                    FilterChip(selected = hasWifi, onClick = { hasWifi = !hasWifi }, label = { Text("Wi-Fi") })
                    FilterChip(selected = hasGenerator, onClick = { hasGenerator = !hasGenerator }, label = { Text("Generator") })
                }
            }
            item {
                Button(
                    onClick = {
                        onSave(
                            existing?.id,
                            title,
                            desc,
                            area,
                            address,
                            roomType,
                            roomsStr.toIntOrNull() ?: 3,
                            rentStr.toIntOrNull() ?: 8500,
                            advStr.toIntOrNull() ?: 17000,
                            serviceStr.toIntOrNull() ?: 500,
                            elecStr.toIntOrNull() ?: 1000,
                            waterStr.toIntOrNull() ?: 200,
                            gasStr.toIntOrNull() ?: 300,
                            netStr.toIntOrNull() ?: 500,
                            0,
                            availFrom,
                            furnished,
                            true,
                            true,
                            hasWifi,
                            false,
                            hasGenerator,
                            imageKeyOrUri,
                            buildList {
                                if (hasWifi) add("High-Speed Wi-Fi")
                                if (hasGenerator) add("Generator Backup")
                                if (furnished) add("Furnished")
                            }.joinToString(",")
                        )
                    },
                    enabled = title.isNotBlank() && address.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("editor_save_btn")
                ) {
                    Text(if (existing == null) "Publish Listing" else "Update Listing")
                }
            }
        }
    }
}
