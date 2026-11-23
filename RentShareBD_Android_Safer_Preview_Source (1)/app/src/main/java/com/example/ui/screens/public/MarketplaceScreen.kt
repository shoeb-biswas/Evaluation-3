package com.example.ui.screens.public

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.DomainLogic
import com.example.ui.components.PropertyCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.AppSnapshot
import com.example.ui.viewmodel.LandlordRentalUiIntent
import com.example.ui.viewmodel.MarketplaceFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    snapshot: AppSnapshot,
    filter: MarketplaceFilter,
    aiRentalState: AiUiState<LandlordRentalUiIntent>,
    onUpdateFilter: ((MarketplaceFilter) -> MarketplaceFilter) -> Unit,
    onResetFilters: () -> Unit,
    onRunAiSearch: (String) -> Unit,
    onOpenProperty: (Long) -> Unit,
    onToggleSave: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var aiPrompt by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    val results = remember(snapshot.properties, snapshot.usersById, filter) {
        val q = filter.searchQuery.trim().lowercase()
        snapshot.properties.filter { p ->
            if (p.verificationStatus != "approved" || p.availabilityStatus == "suspended") return@filter false
            val mq = q.isEmpty() || p.title.lowercase().contains(q) || p.area.lowercase().contains(q) || p.address.lowercase().contains(q)
            val ma = filter.area == "All" || p.area.equals(filter.area, ignoreCase = true)
            val mr = filter.roomType == "All" || p.roomType.equals(filter.roomType, ignoreCase = true)
            val mb = filter.maxRent == null || p.monthlyRent <= filter.maxRent
            val mf = !filter.furnishedOnly || p.furnished
            val mw = !filter.wifiRequired || p.hasWifi
            val lv = snapshot.usersById[p.landlordId]?.identityVerified == true
            val mv = !filter.verifiedOnly || lv
            mq && ma && mr && mb && mf && mw && mv
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Find Verified Flats", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Search by neighborhood, budget, or describe what you need to the AI Rental Assistant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // AI Rental Assistant Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IndigoPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("AI Rental Assistant (Gemini)", style = MaterialTheme.typography.titleSmall, color = IndigoDark)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            placeholder = { Text("e.g. Quiet single room in Mirpur under 9k") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_rental_input")
                        )
                        Button(
                            onClick = { onRunAiSearch(aiPrompt) },
                            enabled = aiPrompt.isNotBlank() && aiRentalState !is AiUiState.Loading,
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .testTag("ai_rental_submit")
                        ) {
                            if (aiRentalState is AiUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PureWhite)
                            } else {
                                Text("Ask AI")
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val suggestions = listOf(
                            "Mirpur single room under 9k",
                            "Uttara flat under 12k",
                            "Bashundhara master suite"
                        )
                        items(suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = {
                                    aiPrompt = suggestion
                                    onRunAiSearch(suggestion)
                                },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    when (aiRentalState) {
                        is AiUiState.Error -> {
                            Surface(color = RoseSurface, shape = MaterialTheme.shapes.small) {
                                Text(
                                    text = aiRentalState.message,
                                    color = RoseError,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        is AiUiState.Success -> {
                            val p = aiRentalState.data.parsed
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                p.area?.let { StatusPill("Area: $it", EmeraldSurface, EmeraldSuccess) }
                                p.maxBudget?.let { StatusPill("Max ৳$it", EmeraldSurface, EmeraldSuccess) }
                                p.roomType?.let { StatusPill(it, IndigoLight, IndigoDark) }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        // Standard Search + Filter button
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search title, road, or neighborhood") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("marketplace_search_input")
                )
                FilledTonalIconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("marketplace_filter_button")
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Open listing filters")
                }
            }
        }

        // Area Quick Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val areas = listOf("All") + DomainLogic.DHAKA_AREAS
                items(areas) { area ->
                    FilterChip(
                        selected = filter.area == area,
                        onClick = { onUpdateFilter { it.copy(area = area) } },
                        label = { Text(area) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${results.size} flat(s) available",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filter != MarketplaceFilter()) {
                    TextButton(onClick = onResetFilters) {
                        Text("Clear Filters")
                    }
                }
            }
        }

        if (results.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(36.dp), tint = SlateSubtle)
                        Text("No listings match your current filters.", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onResetFilters) { Text("Reset Filters") }
                    }
                }
            }
        } else {
            items(results, key = { it.id }) { prop ->
                val verified = snapshot.usersById[prop.landlordId]?.identityVerified == true
                PropertyCard(
                    property = prop,
                    landlordVerified = verified,
                    isSaved = prop.id in snapshot.savedPropertyIds,
                    onToggleSave = { onToggleSave(prop.id) },
                    onClick = { onOpenProperty(prop.id) }
                )
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Filter Flats", style = MaterialTheme.typography.titleLarge)
                Text("Room Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Single Room", "Shared Room", "Master Suite").forEach { rt ->
                        FilterChip(
                            selected = filter.roomType == rt,
                            onClick = { onUpdateFilter { it.copy(roomType = rt) } },
                            label = { Text(rt) }
                        )
                    }
                }
                Text("Max Monthly Rent (BDT)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Any", 8000 to "৳8k", 10000 to "৳10k", 12000 to "৳12k").forEach { (amt, lbl) ->
                        FilterChip(
                            selected = filter.maxRent == amt,
                            onClick = { onUpdateFilter { it.copy(maxRent = amt) } },
                            label = { Text(lbl) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Verified Landlord Only")
                    Switch(checked = filter.verifiedOnly, onCheckedChange = { v -> onUpdateFilter { it.copy(verifiedOnly = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Furnished Only")
                    Switch(checked = filter.furnishedOnly, onCheckedChange = { v -> onUpdateFilter { it.copy(furnishedOnly = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Fiber Wi-Fi Required")
                    Switch(checked = filter.wifiRequired, onCheckedChange = { v -> onUpdateFilter { it.copy(wifiRequired = v) } })
                }
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Show ${results.size} Results")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
