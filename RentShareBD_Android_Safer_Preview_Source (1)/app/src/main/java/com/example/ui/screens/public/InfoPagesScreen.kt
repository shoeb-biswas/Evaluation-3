package com.example.ui.screens.public

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.components.RentShareTopBar
import com.example.ui.theme.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { RentShareTopBar("About RentShare BD", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Smart Rental, Roommate Matching & Shared Living for Bangladesh", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "RentShare BD solves fragmented bachelor housing across Dhaka, Mirpur, Uttara, Dhanmondi, Bashundhara, Savar, and Gazipur with transparent all-in monthly cost calculators, trust scoring, and automated flatmate finance.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                InfoSectionCard(
                    icon = Icons.Filled.Security,
                    title = "Trust & Scam Shield",
                    body = "Listings below 55% of neighborhood median rent or requesting over 4x advance deposit from unverified landlords are automatically flagged for admin moderation."
                )
            }
            item {
                InfoSectionCard(
                    icon = Icons.Filled.People,
                    title = "Weighted Roommate Matching",
                    body = "Compatibility compares budget (20%), location (20%), smoking (15%), cleanliness (15%), sleep (10%), noise (10%), cooking (5%), and guests (5%)."
                )
            }
            item {
                InfoSectionCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Gemini AI Assistance",
                    body = "Natural-language flat search and streaming MoM financial insights powered by Firebase AI Logic."
                )
            }
        }
    }
}

@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    Scaffold(topBar = { RentShareTopBar("How RentShare BD Works", onBack = onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val steps = listOf(
                Triple("1. Explore Verified Flats", "Use filters or the AI Rental Assistant to inspect true monthly rent + DESCO/Titas/WASA/Wi-Fi costs and nearby Dhaka landmarks.", Icons.Filled.Search),
                Triple("2. Match Compatible Roommates", "Complete your lifestyle profile and compare weighted compatibility percentages before moving in together.", Icons.Filled.Diversity3),
                Triple("3. Sign Digital Rental Agreements", "Send applications to landlords, review house rules & notice period terms, and accept agreements with PDF export.", Icons.Filled.Description),
                Triple("4. Manage Shared Life Together", "Form a Roommate Group for real-time chat, equal/custom/percentage expense splitting, minimized settlements, weekly meal voting, and grocery sync.", Icons.Filled.AccountBalanceWallet)
            )
            items(steps.size) { idx ->
                val (title, body, icon) = steps[idx]
                InfoSectionCard(icon = icon, title = title, body = body)
            }
        }
    }
}

@Composable
private fun InfoSectionCard(icon: ImageVector, title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
