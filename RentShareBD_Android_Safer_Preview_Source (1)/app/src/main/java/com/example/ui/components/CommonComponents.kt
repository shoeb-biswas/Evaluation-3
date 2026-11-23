package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.PropertyEntity
import com.example.domain.DomainLogic
import com.example.domain.TotalMonthlyCostBreakdown
import com.example.domain.TrustSummary
import com.example.ui.theme.*

@Composable
fun PropertyImage(
    imageKeyOrUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val drawableRes = when (imageKeyOrUri) {
        "img_hero_dhaka" -> R.drawable.img_hero_dhaka
        "img_prop_uttara" -> R.drawable.img_prop_uttara
        "img_prop_dhanmondi" -> R.drawable.img_prop_dhanmondi
        "img_prop_bashundhara" -> R.drawable.img_prop_bashundhara
        "img_prop_mirpur" -> R.drawable.img_prop_mirpur
        else -> null
    }
    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = imageKeyOrUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.img_prop_mirpur),
            error = painterResource(id = R.drawable.img_prop_mirpur),
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentShareTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PropertyCard(
    property: PropertyEntity,
    landlordVerified: Boolean,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breakdown = DomainLogic.calculateTotalMonthlyCost(
        monthlyRent = property.monthlyRent,
        serviceCharge = property.serviceCharge,
        electricity = property.estimatedElectricity,
        water = property.estimatedWater,
        gas = property.estimatedGas,
        internet = property.estimatedInternet,
        otherCharges = property.otherRecurringCharges
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("property_card_${property.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                PropertyImage(
                    imageKeyOrUri = property.primaryImageKey,
                    contentDescription = property.title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            text = property.area,
                            containerColor = SlateInk.copy(alpha = 0.82f),
                            contentColor = PureWhite,
                            icon = Icons.Filled.LocationOn
                        )
                        if (landlordVerified && !property.isSuspicious) {
                            StatusPill(
                                text = "Verified",
                                containerColor = EmeraldSurface,
                                contentColor = EmeraldSuccess,
                                icon = Icons.Filled.VerifiedUser
                            )
                        }
                        if (property.isSuspicious) {
                            StatusPill(
                                text = "Flagged",
                                containerColor = RoseSurface,
                                contentColor = RoseError,
                                icon = Icons.Filled.Warning
                            )
                        }
                    }
                    FilledIconButton(
                        onClick = onToggleSave,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("save_property_${property.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PureWhite.copy(alpha = 0.92f),
                            contentColor = if (isSaved) IndigoPrimary else SlateInk
                        )
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove saved property" else "Save property"
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${property.roomType} • ${property.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${DomainLogic.formatBdt(property.monthlyRent)} / mo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Est. Total w/ Utilities: ${DomainLogic.formatBdt(breakdown.totalMonthlyCost)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = TealAccent
                        )
                    }
                    StatusPill(
                        text = if (property.furnished) "Furnished" else "Unfurnished",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TotalCostBreakdownCard(
    breakdown: TotalMonthlyCostBreakdown,
    advanceAmount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("True Monthly Living Cost", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            CostRow("Base Monthly Rent", breakdown.rent)
            CostRow("Service Charge", breakdown.serviceCharge)
            CostRow("Estimated Electricity (DESCO/DPDC)", breakdown.electricity)
            CostRow("Estimated Water (WASA)", breakdown.water)
            CostRow("Estimated Gas (Titas)", breakdown.gas)
            CostRow("Fiber Wi-Fi", breakdown.internet)
            if (breakdown.otherCharges > 0) CostRow("Other Charges", breakdown.otherCharges)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estimated Total / Month", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = DomainLogic.formatBdt(breakdown.totalMonthlyCost),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Text(
                text = "Security / Advance Deposit: ${DomainLogic.formatBdt(advanceAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CostRow(label: String, amount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            DomainLogic.formatBdt(amount),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun TrustScoreCard(
    title: String,
    trust: TrustSummary,
    modifier: Modifier = Modifier
) {
    val tierColors = when (trust.tier) {
        "High Trust" -> EmeraldSurface to EmeraldSuccess
        "Standard" -> IndigoLight to IndigoDark
        else -> AmberSurface to AmberWarning
    }
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Text(title, style = MaterialTheme.typography.titleMedium)
                StatusPill(
                    text = "${trust.score}/100 • ${trust.tier}",
                    containerColor = tierColors.first,
                    contentColor = tierColors.second,
                    icon = if (trust.isVerified) Icons.Filled.Verified else Icons.Filled.Shield
                )
            }
            LinearProgressIndicator(
                progress = { trust.score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = tierColors.second,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CheckChip("Email", trust.emailVerified)
                CheckChip("Phone", trust.phoneVerified)
                CheckChip("ID/NID", trust.identityVerified)
            }
            Text(
                text = "${trust.completedRentals} completed rentals • ${trust.reviewCount} reviews (${String.format("%.1f", trust.averageRating)}★)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckChip(label: String, ok: Boolean) {
    StatusPill(
        text = label,
        containerColor = if (ok) EmeraldSurface else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (ok) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
        icon = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Schedule
    )
}

@Composable
fun UserAvatarBadge(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(IndigoLight)
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = IndigoDark
        )
    }
}
