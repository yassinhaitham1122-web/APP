package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.Employee
import com.example.model.PenaltyStatus
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.AttendanceCommitmentGauge
import com.example.ui.components.EmptyStateWidget
import com.example.ui.components.LatenessTrendBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.TimeUtils

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = uiState.language
    val useArabicIndic = uiState.settings.useArabicIndicDigits
    val currency = uiState.settings.currency

    var selectedEmployeeId by remember { mutableStateOf<Long?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<AttendanceStatus?>(null) }
    var reportTab by remember { mutableStateOf(0) } // 0: Overview & Charts, 1: Records Log, 2: Employee Summary

    val empMap = remember(uiState.employees) { uiState.employees.associateBy { it.id } }

    // Filter records
    val filteredRecords = remember(uiState.allRecords, selectedEmployeeId, selectedStatusFilter) {
        uiState.allRecords.filter { r ->
            val empMatch = selectedEmployeeId == null || r.employeeId == selectedEmployeeId
            val statusMatch = selectedStatusFilter == null || r.status == selectedStatusFilter
            empMatch && statusMatch
        }
    }

    // Top late employees calculation (grouped by employeeId, sum lateness)
    val topLateEmployees = remember(uiState.allRecords, empMap) {
        uiState.allRecords
            .filter { it.status == AttendanceStatus.LATE }
            .groupBy { it.employeeId }
            .map { (empId, list) ->
                val emp = empMap[empId]
                val totalLateSec = list.sumOf { it.latenessSeconds }
                val lateCount = list.size
                Triple(emp, totalLateSec, lateCount)
            }
            .filter { it.first != null }
            .sortedByDescending { it.second }
            .take(5)
    }

    // Overall aggregate stats
    val totalRecords = filteredRecords.size
    val onTimeCount = filteredRecords.count { it.status == AttendanceStatus.ON_TIME }
    val lateCount = filteredRecords.count { it.status == AttendanceStatus.LATE }
    val excusedCount = filteredRecords.count { it.status == AttendanceStatus.EXCUSED }
    val absentCount = filteredRecords.count { it.status == AttendanceStatus.ABSENT || it.status == AttendanceStatus.NOT_ARRIVED }
    val totalLatenessSec = filteredRecords.sumOf { it.latenessSeconds }
    val totalPenalties = filteredRecords.sumOf { if (it.penaltyStatus == PenaltyStatus.APPLIED) it.penaltyAmount else 0.0 }
    val totalWorkSec = filteredRecords.sumOf { TimeUtils.calculateWorkDurationSeconds(it.checkInTime, it.checkOutTime) }

    // Prepare lateness trend data (grouped by date)
    val latenessTrendData = remember(uiState.allRecords) {
        uiState.allRecords
            .groupBy { it.date }
            .map { (date, list) ->
                val totalMins = list.sumOf { it.latenessSeconds } / 60
                Pair(date, totalMins)
            }
            .sortedBy { it.first }
            .takeLast(10)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab selector
        item {
            TabRow(
                selectedTabIndex = reportTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(bottom = 6.dp)
            ) {
                Tab(
                    selected = reportTab == 0,
                    onClick = { reportTab = 0 },
                    text = { Text(Translations.get("commitment_rate", lang), fontSize = 12.sp) }
                )
                Tab(
                    selected = reportTab == 1,
                    onClick = { reportTab = 1 },
                    text = { Text(Translations.get("daily_report", lang), fontSize = 12.sp) }
                )
                Tab(
                    selected = reportTab == 2,
                    onClick = { reportTab = 2 },
                    text = { Text(Translations.get("employee_report", lang), fontSize = 12.sp) }
                )
            }
        }

        // Export and Share Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val csv = viewModel.generateCsv(filteredRecords, empMap, lang, currency)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            putExtra(Intent.EXTRA_SUBJECT, "Attendance_Report_${uiState.currentDate}.csv")
                            type = "text/csv"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, Translations.get("export_csv", lang)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("export_csv", lang), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val summaryText = buildString {
                            appendLine("${Translations.get("app_title", lang)} - ${Translations.get("reports_title", lang)}")
                            appendLine("${Translations.get("date", lang)}: ${uiState.currentDate}")
                            appendLine("${Translations.get("stats_present", lang)}: $onTimeCount")
                            appendLine("${Translations.get("stats_late_count", lang)}: $lateCount")
                            appendLine("${Translations.get("stats_excuses_count", lang)}: $excusedCount")
                            appendLine("${Translations.get("stats_total_lateness", lang)}: ${TimeUtils.formatSecondsToHms(totalLatenessSec)}")
                            appendLine("${Translations.get("stats_total_penalties", lang)}: $totalPenalties $currency")
                        }
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, Translations.get("print_share", lang)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_report_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("print_share", lang), fontSize = 12.sp)
                }
            }
        }

        // TAB 0: Charts & Dashboard Overview
        if (reportTab == 0) {
            item {
                AttendanceCommitmentGauge(
                    onTimeCount = onTimeCount,
                    lateCount = lateCount,
                    excusedCount = excusedCount,
                    absentCount = absentCount,
                    language = lang,
                    useArabicIndic = useArabicIndic
                )
            }

            item {
                LatenessTrendBarChart(
                    latenessData = latenessTrendData,
                    language = lang,
                    useArabicIndic = useArabicIndic
                )
            }

            // Top Late Employees Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusLate, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translations.get("top_late_employees", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (topLateEmployees.isEmpty()) {
                            Text(
                                text = Translations.get("no_records_found", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            topLateEmployees.forEach { (emp, totalSec, occurrences) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = emp?.name.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "${formatDigits(occurrences.toString(), useArabicIndic)} ${Translations.get("stats_late_count", lang)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusLateBg
                                    ) {
                                        Text(
                                            text = formatDigits(TimeUtils.formatSecondsToHms(totalSec), useArabicIndic),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = StatusLate,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: Daily Log Records Table
        if (reportTab == 1) {
            // Filter by employee chip row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEmployeeId == null,
                        onClick = { selectedEmployeeId = null },
                        label = { Text(Translations.get("all", lang)) }
                    )
                    uiState.employees.take(3).forEach { e ->
                        FilterChip(
                            selected = selectedEmployeeId == e.id,
                            onClick = { selectedEmployeeId = if (selectedEmployeeId == e.id) null else e.id },
                            label = { Text(e.name.split(" ").firstOrNull() ?: e.name) }
                        )
                    }
                }
            }

            if (filteredRecords.isEmpty()) {
                item {
                    EmptyStateWidget(
                        title = Translations.get("no_records_found", lang),
                        subtitle = Translations.get("daily_report", lang)
                    )
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    val emp = empMap[record.employeeId]
                    RecordItemCard(
                        record = record,
                        employee = emp,
                        language = lang,
                        currency = currency,
                        useArabicIndic = useArabicIndic
                    )
                }
            }
        }

        // TAB 2: Per-Employee Monthly Summary
        if (reportTab == 2) {
            items(uiState.employees, key = { it.id }) { emp ->
                val empRecords = uiState.allRecords.filter { it.employeeId == emp.id }
                val pCount = empRecords.count { it.checkInTime != null }
                val lCount = empRecords.count { it.status == AttendanceStatus.LATE }
                val eCount = empRecords.count { it.status == AttendanceStatus.EXCUSED }
                val totalLateness = empRecords.sumOf { it.latenessSeconds }
                val totalWageDeductions = empRecords.sumOf { if (it.penaltyStatus == PenaltyStatus.APPLIED) it.penaltyAmount else 0.0 }
                val totalHours = empRecords.sumOf { TimeUtils.calculateWorkDurationSeconds(it.checkInTime, it.checkOutTime) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = emp.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${formatDigits(emp.code, useArabicIndic)} • ${emp.department}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(Translations.get("stats_present", lang), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = formatDigits(pCount.toString(), useArabicIndic),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusOnTime
                                )
                            }
                            Column {
                                Text(Translations.get("stats_late_count", lang), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = formatDigits(lCount.toString(), useArabicIndic),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusLate
                                )
                            }
                            Column {
                                Text(Translations.get("stats_total_lateness", lang), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = formatDigits(TimeUtils.formatSecondsToHms(totalLateness), useArabicIndic),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Column {
                                Text(Translations.get("stats_total_penalties", lang), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${formatDigits(totalWageDeductions.toString(), useArabicIndic)} $currency",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusLate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordItemCard(
    record: AttendanceRecord,
    employee: Employee?,
    language: AppLanguage,
    currency: String,
    useArabicIndic: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = employee?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${Translations.get("date", language)}: ${formatDigits(record.date, useArabicIndic)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = record.status, language = language)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${Translations.get("check_in", language)}: ${formatDigits(record.checkInTime ?: "—", useArabicIndic)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${Translations.get("check_out", language)}: ${formatDigits(record.checkOutTime ?: "—", useArabicIndic)}",
                    style = MaterialTheme.typography.labelSmall
                )
                if (record.latenessSeconds > 0) {
                    Text(
                        text = "${Translations.get("lateness_duration", language)}: ${formatDigits(TimeUtils.formatSecondsToHms(record.latenessSeconds), useArabicIndic)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusLate
                    )
                }
            }

            if (record.penaltyStatus == PenaltyStatus.APPLIED) {
                Text(
                    text = "${Translations.get("penalty_amount", language)}: ${formatDigits(record.penaltyAmount.toString(), useArabicIndic)} $currency",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = StatusLate
                )
            }

            if (record.excuseType != null) {
                Text(
                    text = "${Translations.get("status_excused", language)}: ${record.excuseType} (${record.excuseNote ?: ""})",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusExcused
                )
            }
        }
    }
}
