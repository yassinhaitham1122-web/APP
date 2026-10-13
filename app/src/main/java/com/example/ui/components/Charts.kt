package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.ui.theme.*

data class ChartSegment(
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun AttendanceCommitmentGauge(
    onTimeCount: Int,
    lateCount: Int,
    excusedCount: Int,
    absentCount: Int,
    language: AppLanguage,
    useArabicIndic: Boolean,
    modifier: Modifier = Modifier
) {
    val total = onTimeCount + lateCount + excusedCount + absentCount
    val commitmentPercentage = if (total > 0) {
        ((onTimeCount.toFloat() / total.toFloat()) * 100).toInt()
    } else {
        100
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(commitmentPercentage) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Translations.get("commitment_rate", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 22.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background track
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth)
                    )

                    if (total > 0) {
                        var currentAngle = -90f
                        val segments = listOf(
                            Pair(onTimeCount, StatusOnTime),
                            Pair(lateCount, StatusLate),
                            Pair(excusedCount, StatusExcused),
                            Pair(absentCount, StatusAbsent)
                        )

                        for ((count, color) in segments) {
                            if (count > 0) {
                                val sweep = (count.toFloat() / total.toFloat()) * 360f * animatedProgress.value
                                drawArc(
                                    color = color,
                                    startAngle = currentAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                currentAngle += sweep
                            }
                        }
                    } else {
                        // Empty state placeholder arc
                        drawArc(
                            color = StatusOnTime,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress.value,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val percentStr = formatDigits("$commitmentPercentage%", useArabicIndic)
                    Text(
                        text = percentStr,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (commitmentPercentage >= 80) StatusOnTime else if (commitmentPercentage >= 60) StatusExcused else StatusLate
                    )
                    Text(
                        text = formatDigits("$total", useArabicIndic) + " " + Translations.get("records", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = StatusOnTime, label = Translations.get("status_on_time", language), count = onTimeCount, useArabicIndic)
                LegendItem(color = StatusLate, label = Translations.get("status_late", language), count = lateCount, useArabicIndic)
                LegendItem(color = StatusExcused, label = Translations.get("status_excused", language), count = excusedCount, useArabicIndic)
                LegendItem(color = StatusAbsent, label = Translations.get("status_absent", language), count = absentCount, useArabicIndic)
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    count: Int,
    useArabicIndic: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label (${formatDigits(count.toString(), useArabicIndic)})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LatenessTrendBarChart(
    latenessData: List<Pair<String, Int>>, // Date to lateness minutes
    language: AppLanguage,
    useArabicIndic: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = Translations.get("lateness_trend", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (latenessData.isEmpty() || latenessData.all { it.second == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Translations.get("no_records_found", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxMinutes = (latenessData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(10)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = latenessData.size
                        val spacing = 8.dp.toPx()
                        val availableWidth = size.width - (spacing * (barCount + 1))
                        val barWidth = (availableWidth / barCount).coerceIn(12.dp.toPx(), 36.dp.toPx())

                        // Baseline
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, size.height - 20f),
                            end = Offset(size.width, size.height - 20f),
                            strokeWidth = 2f
                        )

                        latenessData.forEachIndexed { index, pair ->
                            val minutes = pair.second
                            val x = spacing + index * (barWidth + spacing)
                            val barHeight = ((minutes.toFloat() / maxMinutes.toFloat()) * (size.height - 35f)).coerceAtLeast(4f)
                            val y = size.height - 20f - barHeight

                            val barColor = if (minutes > 30) StatusLate else if (minutes > 15) StatusExcused else TealPrimary

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${Translations.get("date", language)}: " + latenessData.firstOrNull()?.first?.takeLast(5).orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Max: ${formatDigits(maxMinutes.toString(), useArabicIndic)} ${Translations.get("minutes", language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
