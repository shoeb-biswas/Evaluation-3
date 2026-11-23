package com.example.ui.screens.tenant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.RentShareTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatarBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot

@Composable
fun TenantGroupScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onCreateGroup: (name: String, memberIds: List<Long>) -> Unit,
    onAddMember: (groupId: Long, userId: Long) -> Unit,
    onRemoveMember: (groupId: Long, userId: Long) -> Unit,
    onOpenGroupChat: () -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val members = group?.let { snapshot.membersOfGroup(it.id) } ?: emptyList()
    val candidateTenants = snapshot.users.filter { u -> u.role == "tenant" && members.none { it.id == u.id } }

    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { RentShareTopBar("Roommate Group", onBack = onBack) },
        floatingActionButton = {
            if (group == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Filled.GroupAdd, contentDescription = null) },
                    text = { Text("Create Group") }
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (group == null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No Active Roommate Group", style = MaterialTheme.typography.titleMedium)
                            Text("Create a group to unlock shared expense splitting, utility tracking, meal voting, grocery sync, and group chat.")
                            Button(onClick = { showCreate = true }) { Text("Create Flatmate Group") }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(group.name, style = MaterialTheme.typography.headlineSmall)
                            Text("${members.size} active flatmates sharing bills & living plans", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onOpenGroupChat) {
                                Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Group Chat")
                            }
                        }
                    }
                }
                item { Text("Group Members", style = MaterialTheme.typography.titleMedium) }
                items(members, key = { it.id }) { m ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatarBadge(m.fullName)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(m.fullName, style = MaterialTheme.typography.titleSmall)
                                    Text(m.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (m.id != uid) {
                                IconButton(onClick = { onRemoveMember(group.id, m.id) }) {
                                    Icon(Icons.Filled.PersonRemove, contentDescription = "Remove member", tint = RoseError)
                                }
                            } else {
                                StatusPill("You", IndigoLight, IndigoDark)
                            }
                        }
                    }
                }
                if (candidateTenants.isNotEmpty()) {
                    item { Text("Add Flatmates", style = MaterialTheme.typography.titleMedium) }
                    items(candidateTenants, key = { it.id }) { cand ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cand.fullName, style = MaterialTheme.typography.bodyLarge)
                                TextButton(onClick = { onAddMember(group.id, cand.id) }) { Text("Add") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("Dhaka Shared Flat") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create Roommate Group") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") }) },
            confirmButton = {
                Button(onClick = {
                    onCreateGroup(name, emptyList())
                    showCreate = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TenantGroupChatScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onSendGroupMessage: (groupId: Long, text: String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val group = snapshot.activeGroupFor(uid)
    val msgs = if (group != null) snapshot.messages.filter { it.groupId == group.id } else emptyList()
    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = { RentShareTopBar(group?.name ?: "Group Chat", onBack = onBack) },
        bottomBar = {
            if (group != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Message flatmates...") },
                            modifier = Modifier.weight(1f).testTag("group_chat_input")
                        )
                        FilledIconButton(
                            onClick = {
                                onSendGroupMessage(group.id, input)
                                input = ""
                            },
                            enabled = input.isNotBlank(),
                            modifier = Modifier.size(48.dp).testTag("group_chat_send")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                        }
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (group == null) {
                item { Text("Join or create a Roommate Group first to chat.") }
            }
            items(msgs, key = { it.id }) { m ->
                val isMe = m.senderId == uid
                val sender = snapshot.usersById[m.senderId]?.fullName ?: "Member"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = "$sender • ${m.createdAtLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = if (isMe) IndigoPrimary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isMe) PureWhite else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = if (isMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(m.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TenantPrivateChatScreen(
    initialPeerId: Long?,
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onSendPrivateMessage: (peerId: Long, text: String) -> Unit
) {
    val uid = snapshot.currentUser?.id ?: return
    val peers = snapshot.users.filter { it.id != uid && !it.isSuspended }
    var selectedPeerId by remember(initialPeerId) {
        mutableLongStateOf(initialPeerId ?: peers.firstOrNull()?.id ?: 0L)
    }
    var input by remember { mutableStateOf("") }

    val thread = snapshot.messages.filter {
        it.groupId == null &&
            ((it.senderId == uid && it.receiverId == selectedPeerId) ||
                (it.senderId == selectedPeerId && it.receiverId == uid))
    }

    Scaffold(
        topBar = { RentShareTopBar("Private Messages", onBack = onBack) },
        bottomBar = {
            if (selectedPeerId > 0) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Direct message...") },
                            modifier = Modifier.weight(1f).testTag("private_chat_input")
                        )
                        FilledIconButton(
                            onClick = {
                                onSendPrivateMessage(selectedPeerId, input)
                                input = ""
                            },
                            enabled = input.isNotBlank(),
                            modifier = Modifier.size(48.dp).testTag("private_chat_send")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send direct message")
                        }
                    }
                }
            }
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(peers, key = { it.id }) { peer ->
                    FilterChip(
                        selected = selectedPeerId == peer.id,
                        onClick = { selectedPeerId = peer.id },
                        label = { Text(peer.fullName.substringBefore(" (")) }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (thread.isEmpty()) {
                    item { Text("No messages with this contact yet. Say hello!", style = MaterialTheme.typography.bodySmall) }
                }
                items(thread, key = { it.id }) { m ->
                    val isMe = m.senderId == uid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isMe) TealAccent else MaterialTheme.colorScheme.surface,
                            contentColor = if (isMe) PureWhite else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = if (isMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(m.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}
