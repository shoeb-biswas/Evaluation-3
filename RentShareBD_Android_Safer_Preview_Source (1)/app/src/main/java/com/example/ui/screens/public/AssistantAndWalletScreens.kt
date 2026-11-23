package com.example.ui.screens.public

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.RentShareTopBar
import com.example.ui.viewmodel.AssistantChatMessage

@Composable
fun RentShareAssistantScreen(
    messages: List<AssistantChatMessage>,
    loading: Boolean,
    onBack: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Scaffold(topBar = { RentShareTopBar("Your home, our help", onBack = onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Ask about renting, roommates, and home life. AI can make mistakes; it cannot verify listings or handle payments.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(messages) { _, chat ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement =
                        if (chat.fromUser) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (chat.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(0.90f)
                        ) {
                            Text(
                                chat.text.ifBlank { "Thinking…" },
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(1000) },
                    label = { Text("Ask RentShare AI") },
                    placeholder = { Text("How do I choose a safe shared flat?") },
                    modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                    maxLines = 4
                )
                FilledIconButton(
                    onClick = { onSend(message); message = "" },
                    enabled = message.isNotBlank() && !loading,
                    modifier = Modifier.size(52.dp).testTag("ai_chat_send")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    }
}

@Composable
fun RentShareWalletPreviewScreen(onBack: () -> Unit, onOpenBills: () -> Unit) {
    Scaffold(topBar = { RentShareTopBar("RentShare Wallet · Preview", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp))
                        Text("A simpler way to manage home expenses", style = MaterialTheme.typography.headlineSmall)
                        Text("Concept: 1 RentCoin = ৳1 BDT", style = MaterialTheme.typography.titleLarge)
                        Text("No coins have been issued. This is not a real-money wallet and funds cannot be deposited or spent.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Recharge · Not available", style = MaterialTheme.typography.titleMedium)
                        Text("bKash and Nagad merchant payments will only appear after authorized provider onboarding, settlement verification, and regulatory review.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) { Text("bKash") }
                            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) { Text("Nagad") }
                        }
                        Text("Never send money to a personal number expecting wallet credit.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                Text("Plan your household payments", style = MaterialTheme.typography.titleLarge)
                Text("Track rent, utilities, shared expenses and receipts. A recorded bill is not proof of an actual transfer.",
                    style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = onOpenBills, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View utility bill tracker")
                }
            }
            item {
                Text("Before activation", style = MaterialTheme.typography.titleMedium)
                Text("Licensed payment partner · Secure backend ledger · Verified webhook settlement · Refunds · Payouts · KYC/AML review · Dispute support",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
