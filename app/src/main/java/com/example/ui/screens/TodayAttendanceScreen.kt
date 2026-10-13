package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.DigitalClockBanner
import com.example.ui.components.EmptyStateWidget
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusBadge
import com.example.ui.dialogs.ExcuseDialog
import com.example.ui.dialogs.ManualAttendanceDialog
import com.example.ui.dialogs.PenaltyDialog
import com.example.ui.theme.*
import com.example.util.TimeUtils

@Composable
fun TodayAttendanceScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val lang = uiState.language
    val useArabicIndic = uiState.settings.useArabicIndicDigits
    val currency = uiState.settings.currency

    var activePenaltyRecord by remember { mutableStateOf<Pair<AttendanceRecord, String>?>(null) }
    var activeExcuseRecord by remember { mutableStateOf<Pair<AttendanceRecord, String>?>(null) }
    var activeManualEdit by remember { mutableStateOf<Pair<Employee, AttendanceRecord?>?>(null) }

    // Map records by employee ID
    val todayRecordsMap = remember(uiState.todayRecords) {
        uiState.todayRecords.associateBy { it.employeeId }
    }

    // Filtered employees
    val filteredEmployees = remember(uiState.employees, uiState.searchQuery, uiState.filterStatus, todayRecordsMap) {
        uiState.employees.filter { emp ->
            val matchesQuery = uiState.searchQuery.isBlank() ||
                    emp.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    emp.code.contains(uiState.searchQuery, ignoreCase = true) ||
                    emp.department.contains(uiState.searchQuery, ignoreCase = true)

            val record = todayRecordsMap[emp.id]
            val status = record?.status ?: AttendanceStatus.NOT_ARRIVED
            val matchesStatus = uiState.filterStatus == null || status == uiState.filterStatus

            matchesQuery && matchesStatus && emp.isActive
        }
    }

    // Summary counts
    val presentCount = uiState.todayRecords.count { it.checkInTime != null }
    val lateCount = uiState.todayRecords.count { it.status == AttendanceStatus.LATE }
    val excusedCount = uiState.todayRecords.count { it.status == AttendanceStatus.EXCUSED }
    val activeEmpCount = uiState.employees.count { it.isActive }
    val absentCount = (activeEmpCount - presentCount - excusedCount).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_attendance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Digital Clock Banner
        item {
            DigitalClockBanner(
                currentTime = uiState.currentTime,
                currentDate = uiState.currentDate,
                timezone = uiState.settings.timezone,
                language = lang,
                useArabicIndic = useArabicIndic
            )
        }

        // 2. Missing Checkout Warning Banner
        if (uiState.missingCheckOutCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("missing_checkout_banner"),
                    colors = CardDefaults.cardColors(containerColor = StatusLateBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusLate
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Translations.get("missing_checkout_warning", lang),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusLate
                            )
                            Text(
                                text = "${formatDigits(uiState.missingCheckOutCount.toString(), useArabicIndic)} ${Translations.get("records", lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusLate.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Metric KPI Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricStatCard(
                    title = Translations.get("status_present", lang),
                    value = formatDigits(presentCount.toString(), useArabicIndic),
                    icon = Icons.Default.CheckCircle,
                    color = StatusOnTime,
                    bgColor = StatusOnTimeBg,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter(if (uiState.filterStatus == AttendanceStatus.ON_TIME) null else AttendanceStatus.ON_TIME) }
                )
                MetricStatCard(
                    title = Translations.get("status_late", lang),
                    value = formatDigits(lateCount.toString(), useArabicIndic),
                    icon = Icons.Default.Schedule,
                    color = StatusLate,
                    bgColor = StatusLateBg,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter(if (uiState.filterStatus == AttendanceStatus.LATE) null else AttendanceStatus.LATE) }
                )
                MetricStatCard(
                    title = Translations.get("status_excused", lang),
                    value = formatDigits(excusedCount.toString(), useArabicIndic),
                    icon = Icons.Default.VerifiedUser,
                    color = StatusExcused,
                    bgColor = StatusExcusedBg,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter(if (uiState.filterStatus == AttendanceStatus.EXCUSED) null else AttendanceStatus.EXCUSED) }
                )
                MetricStatCard(
                    title = Translations.get("status_absent", lang),
                    value = formatDigits(absentCount.toString(), useArabicIndic),
                    icon = Icons.Default.PersonOff,
                    color = StatusAbsent,
                    bgColor = StatusAbsentBg,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter(if (uiState.filterStatus == AttendanceStatus.ABSENT) null else AttendanceStatus.ABSENT) }
                )
            }
        }

        // 4. Search and Filter Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(Translations.get("search", lang)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employee_search_field"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // Filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = uiState.filterStatus == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text(Translations.get("all", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == AttendanceStatus.ON_TIME,
                        onClick = { viewModel.setStatusFilter(AttendanceStatus.ON_TIME) },
                        label = { Text(Translations.get("status_on_time", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == AttendanceStatus.LATE,
                        onClick = { viewModel.setStatusFilter(AttendanceStatus.LATE) },
                        label = { Text(Translations.get("status_late", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == AttendanceStatus.EXCUSED,
                        onClick = { viewModel.setStatusFilter(AttendanceStatus.EXCUSED) },
                        label = { Text(Translations.get("status_excused", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == AttendanceStatus.NOT_ARRIVED,
                        onClick = { viewModel.setStatusFilter(AttendanceStatus.NOT_ARRIVED) },
                        label = { Text(Translations.get("status_not_arrived", lang)) }
                    )
                }
            }
        }

        // 5. Employee List
        if (filteredEmployees.isEmpty()) {
            item {
                EmptyStateWidget(
                    title = Translations.get("no_records_found", lang),
                    subtitle = Translations.get("search", lang)
                )
            }
        } else {
            items(filteredEmployees, key = { it.id }) { employee ->
                val record = todayRecordsMap[employee.id]
                EmployeeAttendanceCard(
                    employee = employee,
                    record = record,
                    language = lang,
                    currency = currency,
                    useArabicIndic = useArabicIndic,
                    onCheckIn = { viewModel.checkIn(employee) },
                    onCheckOut = { viewModel.checkOut(employee) },
                    onApplyPenalty = { r -> activePenaltyRecord = Pair(r, employee.name) },
                    onAcceptExcuse = { r -> activeExcuseRecord = Pair(r, employee.name) },
                    onManualEdit = { activeManualEdit = Pair(employee, record) }
                )
            }
        }
    }

    // Dialogs
    activePenaltyRecord?.let { (rec, empName) ->
        PenaltyDialog(
            employeeName = empName,
            record = rec,
            currency = currency,
            language = lang,
            useArabicIndic = useArabicIndic,
            onConfirm = { amount, reason ->
                viewModel.applyPenalty(rec.id, empName, amount, reason)
                activePenaltyRecord = null
            },
            onDismiss = { activePenaltyRecord = null }
        )
    }

    activeExcuseRecord?.let { (rec, empName) ->
        ExcuseDialog(
            employeeName = empName,
            record = rec,
            language = lang,
            useArabicIndic = useArabicIndic,
            onConfirm = { excuseType, note ->
                viewModel.acceptExcuse(rec.id, empName, excuseType, note)
                activeExcuseRecord = null
            },
            onDismiss = { activeExcuseRecord = null }
        )
    }

    activeManualEdit?.let { (emp, rec) ->
        ManualAttendanceDialog(
            employee = emp,
            record = rec,
            currentDate = uiState.currentDate,
            language = lang,
            onConfirm = { updatedRec, reason ->
                viewModel.saveManualAttendance(updatedRec, emp.name, reason)
                activeManualEdit = null
            },
            onDismiss = { activeManualEdit = null }
        )
    }
}

@Composable
fun EmployeeAttendanceCard(
    employee: Employee,
    record: AttendanceRecord?,
    language: AppLanguage,
    currency: String,
    useArabicIndic: Boolean,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onApplyPenalty: (AttendanceRecord) -> Unit,
    onAcceptExcuse: (AttendanceRecord) -> Unit,
    onManualEdit: () -> Unit
) {
    val isCheckedIn = record?.checkInTime != null
    val isCheckedOut = record?.checkOutTime != null
    val isLate = record?.status == AttendanceStatus.LATE
    val isExcused = record?.status == AttendanceStatus.EXCUSED
    val status = when {
        isExcused -> AttendanceStatus.EXCUSED
        isLate -> AttendanceStatus.LATE
        isCheckedIn -> AttendanceStatus.ON_TIME
        else -> AttendanceStatus.NOT_ARRIVED
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_card_${employee.code}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLate) StatusLateBg.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Name, Code & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initials Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isLate) StatusLate else TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.take(2).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${formatDigits(employee.code, useArabicIndic)} • ${employee.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = status, language = language)

                IconButton(
                    onClick = onManualEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("manual_edit_btn_${employee.code}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = Translations.get("manual_entry", language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Timestamps Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StatusOnTime
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${Translations.get("check_in", language)}: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isCheckedIn) formatDigits(record?.checkInTime.orEmpty(), useArabicIndic) else "—",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCheckedIn) StatusOnTime else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = IndigoSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${Translations.get("check_out", language)}: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isCheckedOut) formatDigits(record?.checkOutTime.orEmpty(), useArabicIndic) else "—",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCheckedOut) IndigoSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Lateness Highlight Alert Banner
            if (isLate && record != null) {
                val latenessStr = TimeUtils.formatSecondsToHms(record.latenessSeconds)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusLateBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = StatusLate,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${Translations.get("lateness_duration", language)}: ${formatDigits(latenessStr, useArabicIndic)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusLate
                            )
                        }

                        if (record.penaltyStatus == PenaltyStatus.APPLIED) {
                            Text(
                                text = "${Translations.get("penalty_amount", language)}: ${formatDigits(record.penaltyAmount.toString(), useArabicIndic)} $currency",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusLate
                            )
                        }
                    }
                }

                // Penalty & Excuse Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onApplyPenalty(record) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusLate),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_penalty_btn_${employee.code}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translations.get("apply_penalty", language), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { onAcceptExcuse(record) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusExcused),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("accept_excuse_btn_${employee.code}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translations.get("accept_excuse", language), fontSize = 12.sp)
                    }
                }
            }

            // Excused Note Banner
            if (isExcused && record != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusExcusedBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = StatusExcused,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${Translations.get("status_excused", language)} (${record.excuseType ?: ""})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusExcused
                        )
                    }
                }
            }

            // Primary Check In / Check Out Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCheckIn,
                    enabled = !isCheckedIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusOnTime,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("check_in_btn_${employee.code}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.get("check_in", language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onCheckOut,
                    enabled = isCheckedIn && !isCheckedOut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("check_out_btn_${employee.code}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.get("check_out", language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
