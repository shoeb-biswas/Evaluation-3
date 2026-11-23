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
import com.example.data.local.UserEntity
import com.example.domain.DomainLogic
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatarBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot

@Composable
fun RoommateDiscoveryScreen(
    snapshot: AppSnapshot,
    onSendRequest: (receiverId: Long, message: String) -> Unit,
    onMessagePeer: (Long) -> Unit,
    onRequireAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var areaFilter by remember { mutableStateOf("All") }
    var nonSmokerOnly by remember { mutableStateOf(false) }
    var requestTarget by remember { mutableStateOf<UserEntity?>(null) }

    val baseUserId = snapshot.currentUser?.id ?: snapshot.users.firstOrNull { it.role == "tenant" }?.id ?: 1L
    val candidates = remember(snapshot.users, snapshot.tenantProfiles, snapshot.preferences, baseUserId, areaFilter, nonSmokerOnly) {
        snapshot.users
            .filter { it.role == "tenant" && it.id != baseUserId && !it.isSuspended }
            .map { user ->
                val tp = snapshot.tenantProfiles.firstOrNull { it.userId == user.id }
                val pref = snapshot.preferences.firstOrNull { it.userId == user.id }
                val compat = snapshot.compatibilityBetween(baseUserId, user.id)
                val trust = snapshot.trustForUser(user.id)
                Triple(user, tp to pref, compat to trust)
            }
            .filter { (_, tpPref, _) ->
                val area = tpPref.second?.preferredLocation ?: tpPref.first?.preferredLocation ?: ""
                val smoking = tpPref.second?.smokingPreference ?: "non_smoker"
                (areaFilter == "All" || area.equals(areaFilter, ignoreCase = true)) &&
                    (!nonSmokerOnly || smoking == "non_smoker")
            }
            .sortedByDescending { it.third.first.score }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Roommate Compatibility", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "8-factor lifestyle matching across budget, area, smoking, cleanliness, sleep, noise, cooking & guests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All") + DomainLogic.DHAKA_AREAS) { area ->
                    FilterChip(
                        selected = areaFilter == area,
                        onClick = { areaFilter = area },
                        label = { Text(area) }
                    )
                }
                item {
                    FilterChip(
                        selected = nonSmokerOnly,
                        onClick = { nonSmokerOnly = !nonSmokerOnly },
                        label = { Text("Non-Smokers") }
                    )
                }
            }
        }

        items(candidates, key = { it.first.id }) { (candidate, tpPref, compatTrust) ->
            val (tp, pref) = tpPref
            val (compat, trust) = compatTrust
            val alreadyRequested = snapshot.currentUser?.let { cu ->
                snapshot.roommateRequests.any {
                    (it.senderId == cu.id && it.receiverId == candidate.id) ||
                        (it.receiverId == cu.id && it.senderId == candidate.id)
                }
            } ?: false

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            UserAvatarBadge(candidate.fullName)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(candidate.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${tp?.occupation ?: "Tenant"} • ${tp?.institutionOrWorkplace ?: "Dhaka"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(
                            text = "${compat.score}% Match",
                            containerColor = if (compat.score >= 75) EmeraldSurface else IndigoLight,
                            contentColor = if (compat.score >= 75) EmeraldSuccess else IndigoDark
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = "${pref?.preferredLocation ?: tp?.preferredLocation ?: "Dhaka"}",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Filled.LocationOn
                        )
                        StatusPill(
                            text = "Budget ${DomainLogic.formatBdt(pref?.monthlyBudget ?: tp?.monthlyBudget ?: 9000)}",
                            containerColor = TealLight.copy(alpha = 0.6f),
                            contentColor = TealDark
                        )
                        StatusPill(
                            text = "Trust ${trust.score}",
                            containerColor = IndigoLight.copy(alpha = 0.5f),
                            contentColor = IndigoDark,
                            icon = Icons.Filled.VerifiedUser
                        )
                    }

                    if (!tp?.bio.isNullOrBlank()) {
                        Text(tp?.bio.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }

                    if (compat.matches.isNotEmpty()) {
                        Text(
                            "Matches: ${compat.matches.take(3).joinToString(" • ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    }
                    if (compat.differences.isNotEmpty()) {
                        Text(
                            "Differences: ${compat.differences.take(2).joinToString(" • ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                if (snapshot.currentUser == null) onRequireAuth()
                                else onMessagePeer(candidate.id)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Message")
                        }
                        Button(
                            onClick = {
                                if (snapshot.currentUser == null) onRequireAuth()
                                else requestTarget = candidate
                            },
                            enabled = !alreadyRequested,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("connect_roommate_${candidate.id}")
                        ) {
                            Text(if (alreadyRequested) "Connected / Sent" else "Connect")
                        }
                    }
                }
            }
        }
    }

    requestTarget?.let { target ->
        var msg by remember { mutableStateOf("Hi ${target.fullName.substringBefore(" ")}, we have a great compatibility match! Interested in teaming up?") }
        AlertDialog(
            onDismissRequest = { requestTarget = null },
            title = { Text("Connect with ${target.fullName}") },
            text = {
                OutlinedTextField(
                    value = msg,
                    onValueChange = { msg = it },
                    label = { Text("Introductory Message") },
                    minLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSendRequest(target.id, msg)
                    requestTarget = null
                }) { Text("Send Request") }
            },
            dismissButton = { TextButton(onClick = { requestTarget = null }) { Text("Cancel") } }
        )
    }
}
