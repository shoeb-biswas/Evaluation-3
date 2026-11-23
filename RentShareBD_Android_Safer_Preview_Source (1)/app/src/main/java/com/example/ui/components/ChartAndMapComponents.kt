package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DomainLogic
import com.example.domain.NearbyLandmarkResult
import com.example.ui.theme.*
import kotlin.math.max

@Composable
fun DhakaLocationMapCanvas(
    propertyArea: String,
    propertyLat: Double,
    propertyLng: Double,
    modifier: Modifier = Modifier
) {
    val landmarks = remember(propertyLat, propertyLng) {
        DomainLogic.computeNearbyLandmarks(propertyLat, propertyLng)
    }
    var selected by remember(landmarks) { mutableStateOf(landmarks.firstOrNull()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = IndigoPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Dhaka Neighborhood & Transit Radar ($propertyArea)", style = MaterialTheme.typography.titleMedium)
            }

            val gridColor = SlateBorder
            val propertyColor = IndigoPrimary
            val landmarkColor = TealAccent
            val selectedColor = AmberWarning

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                drawRect(color = Color(0xFFF1F5F9), size = size)

                val stepX = size.width / 6f
                val stepY = size.height / 4f
                for (i in 1..5) {
                    drawLine(gridColor, Offset(stepX * i, 0f), Offset(stepX * i, size.height), strokeWidth = 1f)
                }
                for (j in 1..3) {
                    drawLine(gridColor, Offset(0f, stepY * j), Offset(size.width, stepY * j), strokeWidth = 1f)
                }

                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = propertyColor.copy(alpha = 0.12f),
                    radius = minOf(size.width, size.height) * 0.36f,
                    center = center
                )

                val maxDelta = landmarks.maxOfOrNull {
                    maxOf(kotlin.math.abs(it.landmark.lat - propertyLat), kotlin.math.abs(it.landmark.lng - propertyLng))
                }?.coerceAtLeast(0.02) ?: 0.05

                landmarks.forEach { item ->
                    val dx = ((item.landmark.lng - propertyLng) / maxDelta).toFloat().coerceIn(-1f, 1f)
                    val dy = (-(item.landmark.lat - propertyLat) / maxDelta).toFloat().coerceIn(-1f, 1f)
                    val pt = Offset(
                        center.x + dx * (size.width * 0.40f),
                        center.y + dy * (size.height * 0.38f)
                    )
                    drawLine(
                        color = SlateBorder,
                        start = center,
                        end = pt,
                        strokeWidth = 2f
                    )
                    val isSel = selected?.landmark?.name == item.landmark.name
                    drawCircle(
                        color = if (isSel) selectedColor else landmarkColor,
                        radius = if (isSel) 11f else 8f,
                        center = pt
                    )
                }

                // Property pin at center
                drawCircle(color = PureWhite, radius = 14f, center = center)
                drawCircle(color = propertyColor, radius = 10f, center = center)
            }

            selected?.let { sel ->
                Surface(
                    color = IndigoLight.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${sel.landmark.name} (${sel.landmark.type})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${sel.distanceKm} km away • ~${sel.transit.estimatedMinutes} min commute",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TransitFareChip("Bus", sel.transit.busFareBdt)
                            TransitFareChip("Rickshaw", sel.transit.rickshawFareBdt)
                            TransitFareChip("CNG", sel.transit.cngFareBdt)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                landmarks.take(5).forEach { lm ->
                    val isSelected = selected?.landmark?.name == lm.landmark.name
                    Surface(
                        color = if (isSelected) TealLight.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = lm }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lm.landmark.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${lm.landmark.type} • ~${lm.transit.estimatedMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${lm.distanceKm} km",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                color = IndigoPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitFareChip(mode: String, fare: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.DirectionsBus, contentDescription = null, modifier = Modifier.size(14.dp), tint = IndigoPrimary)
        Spacer(Modifier.width(4.dp))
        Text(
            "$mode ৳$fare",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun CategoryBreakdownChart(
    title: String,
    items: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = max(1, items.values.sum())
    val palette = listOf(IndigoPrimary, TealAccent, AmberWarning, EmeraldSuccess, IndigoDark, RoseError)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) {
                Text("No expense records yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.entries.sortedByDescending { it.value }.forEachIndexed { idx, (label, amount) ->
                    val ratio = (amount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    val barColor = palette[idx % palette.size]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${DomainLogic.formatBdt(amount)} (${(ratio * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            drawRoundRect(
                                color = SlateSurfaceVariant,
                                size = size,
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width * ratio, size.height),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarComparisonChart(
    title: String,
    bars: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxVal = max(1, bars.maxOfOrNull { it.second } ?: 1)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (bars.isEmpty()) {
                Text("No chart data available.", style = MaterialTheme.typography.bodySmall)
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val slotW = size.width / bars.size.coerceAtLeast(1)
                    val barW = (slotW * 0.55f).coerceAtLeast(12f)
                    bars.forEachIndexed { i, (_, value) ->
                        val h = (value.toFloat() / maxVal.toFloat()) * (size.height - 16f)
                        val x = i * slotW + (slotW - barW) / 2f
                        val y = size.height - h
                        drawRoundRect(
                            color = if (i % 2 == 0) IndigoPrimary else TealAccent,
                            topLeft = Offset(x, y),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bars.forEach { (label, value) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                DomainLogic.formatBdt(value),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = IndigoPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
