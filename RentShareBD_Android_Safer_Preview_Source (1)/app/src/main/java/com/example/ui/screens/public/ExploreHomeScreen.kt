package com.example.ui.screens.public

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.domain.DomainLogic
import com.example.domain.UserRole
import com.example.ui.components.PropertyCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppSnapshot

@Composable
fun ExploreHomeScreen(
    snapshot: AppSnapshot,
    onOpenMarketplace: (String?) -> Unit,
    onOpenProperty: (Long) -> Unit,
    onToggleSave: (Long) -> Unit,
    onOpenRoommates: () -> Unit,
    onOpenAssistant: () -> Unit,
    onDemoSwitch: (UserRole) -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHowItWorks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featured = snapshot.properties.filter {
        it.verificationStatus == "approved" && it.availabilityStatus != "suspended"
    }.take(4)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(276.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_dhaka),
                    contentDescription = "Dhaka shared living apartments",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SlateInk.copy(alpha = 0.55f),
                                    SlateInk.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = "RentShare BD",
                            containerColor = IndigoPrimary,
                            contentColor = PureWhite,
                            icon = Icons.Filled.Apartment
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onOpenHowItWorks,
                                colors = ButtonDefaults.textButtonColors(contentColor = PureWhite),
                                modifier = Modifier.testTag("home_how_it_works_btn")
                            ) {
                                Text("How It Works")
                            }
                            if (snapshot.currentUser == null) {
                                FilledTonalButton(
                                    onClick = onOpenAuth,
                                    modifier = Modifier.testTag("home_sign_in_btn")
                                ) {
                                    Text("Sign In")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = onOpenWorkspace,
                                    modifier = Modifier.testTag("home_workspace_btn")
                                ) {
                                    Text(UserRole.fromKey(snapshot.currentUser.role).label)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.tagline),
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite
                        )
                        Text(
                            text = "Find a comfortable place, kind flatmates, and a little more belonging. Preview listings are examples, not verified live inventory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoLight
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onOpenMarketplace(null) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("explore_flats_cta")
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Browse Flats")
                            }
                            OutlinedButton(
                                onClick = onOpenRoommates,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("find_roommates_cta")
                            ) {
                                Text("Match Roommates")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clickable { onOpenAssistant() }.testTag("home_ai_chat"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.weight(1f)) {
                        Text("A friendly guide for finding home", style = MaterialTheme.typography.titleMedium)
                        Text("Ask RentShare AI about rooms, flatmates, moving, or budgeting.", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }

        // Instant Demo Switcher Card: never shown to public release builds.
        if (BuildConfig.DEBUG) item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (snapshot.currentUser != null)
                                    "Active: ${snapshot.currentUser.fullName}"
                                else
                                    "Instant Demo Workspaces",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try Tenant shared living, Landlord portal, or Admin trust moderation in 1 tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            val active = snapshot.currentUser?.role == role.key
                            FilterChip(
                                selected = active,
                                onClick = { onDemoSwitch(role) },
                                label = { Text(role.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.TENANT -> Icons.Filled.Person
                                            UserRole.LANDLORD -> Icons.Filled.HomeWork
                                            UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("demo_role_${role.key}")
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Welcome home", style = MaterialTheme.typography.titleLarge)
                    Text("A calm place to find your next chapter. This version uses sample content and on-device storage; public bookings and real payments are not enabled.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Dhaka Neighborhoods
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Popular Dhaka Areas",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(DomainLogic.DHAKA_AREAS) { area ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clickable { onOpenMarketplace(area) }
                                .testTag("area_chip_$area")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationCity, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(area, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // Featured Verified Flats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Featured Verified Flats", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onOpenMarketplace(null) }) {
                    Text("View all (${featured.size}+)")
                }
            }
        }

        items(featured, key = { it.id }) { prop ->
            val landlordVerified = snapshot.usersById[prop.landlordId]?.identityVerified == true
            PropertyCard(
                property = prop,
                landlordVerified = landlordVerified,
                isSaved = prop.id in snapshot.savedPropertyIds,
                onToggleSave = { onToggleSave(prop.id) },
                onClick = { onOpenProperty(prop.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Platform Value Pillars + About Link
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = TealLight.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Built for Safe Shared Living in Bangladesh", style = MaterialTheme.typography.titleMedium, color = TealDark)
                    Text(
                        "• Scam Shield flags suspicious deposits (>4x rent) & unverified landlords\n• 8-Factor Roommate Compatibility Engine\n• Split Expenses, Utility Reminders, Weekly Meals & Gemini AI Coach",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateInk
                    )
                    TextButton(onClick = onOpenAbout) {
                        Text("About RentShare BD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
