package com.example.ui.screens.tenant

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.GroceryItemEntity
import com.example.data.local.MealItemEntity
import com.example.domain.DomainLogic
import com.example.domain.UserRole
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.AppSnapshot

@Composable
fun TenantMealsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onAddMeal: (groupId: Long, planId: Long?, day: String, slot: String, name: String, desc: String) -> Unit,
    onVoteMeal: (MealItemEntity) -> Unit,
    onDeleteMeal: (Long) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val plan = group?.let { g -> snapshot.mealPlans.firstOrNull { it.groupId == g.id } }
    val allMeals = plan?.let { p -> snapshot.mealItems.filter { it.mealPlanId == p.id } } ?: emptyList()

    var selectedDay by remember { mutableStateOf("Saturday") }
    var showAdd by remember { mutableStateOf(false) }

    val dayMeals = allMeals.filter { it.dayOfWeek == selectedDay }

    Scaffold(
        topBar = { RentShareTopBar("Weekly Meal Planner", onBack = onBack) },
        floatingActionButton = {
            if (group != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Meal") }
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DomainLogic.MEAL_DAYS) { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.take(3)) }
                        )
                    }
                }
            }
            if (dayMeals.isEmpty()) {
                item { Text("No meals planned for $selectedDay yet. Tap Add Meal.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(dayMeals, key = { it.id }) { meal ->
                val voted = snapshot.mealParticipants.any { it.mealItemId == meal.id && it.userId == uid && it.voted }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusPill(meal.mealSlot, TealLight, TealDark)
                            Text(meal.mealName, style = MaterialTheme.typography.titleMedium)
                            if (meal.description.isNotBlank()) {
                                Text(meal.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = { onVoteMeal(meal) }, enabled = !voted) {
                                Icon(Icons.Filled.ThumbUp, contentDescription = "Vote meal", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${meal.votes}")
                            }
                            IconButton(onClick = { onDeleteMeal(meal.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete meal")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd && group != null) {
        var slot by remember { mutableStateOf("Lunch") }
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Meal ($selectedDay)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DomainLogic.MEAL_SLOTS.forEach { s ->
                            FilterChip(selected = slot == s, onClick = { slot = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Details") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddMeal(group.id, plan?.id, selectedDay, slot, name, desc)
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TenantGroceriesScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onAddItem: (groupId: Long, listId: Long?, name: String, qty: String, price: Double, assignedTo: Long?) -> Unit,
    onToggleItem: (GroceryItemEntity) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onSyncToExpense: (groupId: Long, purchased: List<GroceryItemEntity>, memberIds: List<Long>) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val members = group?.let { snapshot.membersOfGroup(it.id) } ?: emptyList()
    val gList = group?.let { g -> snapshot.groceryLists.firstOrNull { it.groupId == g.id } }
    val items = gList?.let { l -> snapshot.groceryItems.filter { it.groceryListId == l.id } } ?: emptyList()
    val purchased = items.filter { it.isPurchased }

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { RentShareTopBar("Shared Grocery List", onBack = onBack) },
        floatingActionButton = {
            if (group != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Filled.AddShoppingCart, contentDescription = null) },
                    text = { Text("Add Item") }
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (group != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealLight.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Purchased Total: ${DomainLogic.formatBdt(purchased.sumOf { it.estimatedPrice })}", style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = { onSyncToExpense(group.id, purchased, members.map { it.id }) },
                                enabled = purchased.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("sync_groceries_btn")
                            ) {
                                Text("Sync ${purchased.size} Purchased Items to Expenses")
                            }
                        }
                    }
                }
            }
            items(items, key = { it.id }) { item ->
                val assignee = item.assignedToUserId?.let { snapshot.usersById[it]?.fullName?.substringBefore(" ") } ?: "Any"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.isPurchased, onCheckedChange = { onToggleItem(item) })
                        Column(Modifier.weight(1f)) {
                            Text(item.itemName, style = MaterialTheme.typography.titleSmall)
                            Text("${item.quantity} • Assignee: $assignee", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(DomainLogic.formatBdt(item.estimatedPrice), style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily))
                        IconButton(onClick = { onDeleteItem(item.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete grocery item")
                        }
                    }
                }
            }
        }
    }

    if (showAdd && group != null) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1 kg") }
        var price by remember { mutableStateOf("250") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Grocery Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Estimated Price (৳)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddItem(group.id, gList?.id, name, qty, price.toDoubleOrNull() ?: 0.0, uid)
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TenantMaintenanceScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onSubmitTicket: (propertyId: Long, landlordId: Long, category: String, title: String, desc: String, priority: String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val myTickets = snapshot.maintenanceRequests.filter { it.tenantId == uid }
    val activeProp = snapshot.agreements.firstOrNull { it.tenantId == uid }?.let { ag ->
        snapshot.properties.firstOrNull { it.id == ag.propertyId }
    } ?: snapshot.properties.firstOrNull()

    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { RentShareTopBar("Maintenance Tickets", onBack = onBack) },
        floatingActionButton = {
            if (activeProp != null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                    text = { Text("Report Issue") }
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(myTickets, key = { it.id }) { ticket ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ticket.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusPill(ticket.status, IndigoLight, IndigoDark)
                        }
                        Text("${ticket.category} • Priority: ${ticket.priority}", style = MaterialTheme.typography.labelMedium)
                        Text(ticket.description, style = MaterialTheme.typography.bodySmall)
                        if (ticket.landlordResponse.isNotBlank()) {
                            Surface(color = EmeraldSurface, shape = MaterialTheme.shapes.small) {
                                Text("Landlord: ${ticket.landlordResponse}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate && activeProp != null) {
        var cat by remember { mutableStateOf("Plumbing") }
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Medium") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Maintenance Ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category (Plumbing/Electrical/Wi-Fi)") })
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Summary") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubmitTicket(activeProp.id, activeProp.landlordId, cat, title, desc, priority)
                    showCreate = false
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TenantAiCoachScreen(
    snapshot: AppSnapshot,
    coachState: AiUiState<String>,
    onBack: () -> Unit,
    onAskCoach: (String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val summary = snapshot.financialSummaryForGroup(group?.id ?: 1L)
    var question by remember { mutableStateOf("") }

    Scaffold(topBar = { RentShareTopBar("AI Financial Coach", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryChipCard("This Month", DomainLogic.formatBdt(summary.currentMonthTotal), Modifier.weight(1f))
                    SummaryChipCard("Last Month", DomainLogic.formatBdt(summary.previousMonthTotal), Modifier.weight(1f))
                    SummaryChipCard(
                        "Predicted",
                        summary.predictedNextMonth?.let { DomainLogic.formatBdt(it) } ?: "N/A",
                        Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IndigoPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Ask Gemini About Your Shared Budget", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            placeholder = { Text("e.g. How can our Mirpur flat cut food & electricity costs next month?") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("ai_coach_input")
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "How can we cut 15% next month?",
                                "Explain our MoM spending spike",
                                "Budget split tip for Dhaka students"
                            ).forEach { q ->
                                item {
                                    SuggestionChip(
                                        onClick = {
                                            question = q
                                            onAskCoach(q)
                                        },
                                        label = { Text(q) }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { onAskCoach(question) },
                            enabled = coachState !is AiUiState.Loading,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_coach_submit")
                        ) {
                            Text(if (coachState is AiUiState.Loading || coachState is AiUiState.Streaming) "Generating..." else "Analyze with AI Coach")
                        }
                    }
                }
            }

            item {
                when (coachState) {
                    is AiUiState.Idle -> Unit
                    is AiUiState.Loading -> {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is AiUiState.Streaming -> {
                        Card(colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.25f))) {
                            Text(coachState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                        }
                    }
                    is AiUiState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.35f))
                        ) {
                            Text(coachState.data, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                        }
                    }
                    is AiUiState.Error -> {
                        Surface(color = RoseSurface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, RoseError.copy(alpha = 0.35f))) {
                            Text(coachState.message, color = RoseError, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChipCard(label: String, valText: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valText, style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily), color = IndigoPrimary)
        }
    }
}

@Composable
fun TenantNotificationsScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onMarkAllRead: (Long) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val list = snapshot.notifications.filter { it.userId == uid }

    Scaffold(
        topBar = {
            RentShareTopBar(
                title = "Notifications",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { onMarkAllRead(uid) }) { Text("Mark All Read") }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (list.isEmpty()) item { Text("You're all caught up!") }
            items(list, key = { it.id }) { n ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (n.isRead) MaterialTheme.colorScheme.surface else IndigoLight.copy(alpha = 0.28f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall)
                        Text(n.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun TenantSettingsScreen(
    snapshot: AppSnapshot,
    onBack: (() -> Unit)?,
    onChangePassword: (Long, String, String) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit,
    onLogout: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val user = snapshot.currentUser
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    Scaffold(topBar = { RentShareTopBar("Account & Settings", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (user == null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Not Signed In", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = onOpenAuth, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Sign In or Register")
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(user.fullName, style = MaterialTheme.typography.titleLarge)
                            Text("${user.email} • Role: ${user.role.uppercase()}", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Change Password", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(value = currentPass, onValueChange = { currentPass = it }, label = { Text("Current Password") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("New Password") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onChangePassword(user.id, currentPass, newPass)
                                    currentPass = ""
                                    newPass = ""
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) { Text("Update Password") }
                        }
                    }
                }
            }
            if (BuildConfig.DEBUG) item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quick Demo Role Switch", style = MaterialTheme.typography.titleMedium)
                        UserRole.entries.forEach { r ->
                            OutlinedButton(onClick = { onSwitchDemoRole(r) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Switch to Demo ${r.label}")
                            }
                        }
                    }
                }
            }
        }
    }
}
