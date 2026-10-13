package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.model.AttendanceStatus
import com.example.model.Employee
import com.example.ui.AppScreen
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.dialogs.PinDialog
import com.example.ui.theme.*

@Composable
fun KioskScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val lang = uiState.language
    val useArabicIndic = uiState.settings.useArabicIndicDigits

    var enteredCode by remember { mutableStateOf("") }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    // Map today's records
    val todayMap = remember(uiState.todayRecords) {
        uiState.todayRecords.associateBy { it.employeeId }
    }

    // Auto-select when code matches
    LaunchedEffect(enteredCode) {
        if (enteredCode.isNotBlank()) {
            val trimmed = enteredCode.trim()
            val match = uiState.employees.find {
                it.isActive && (it.code.equals(trimmed, ignoreCase = true) || it.code.endsWith(trimmed))
            }
            if (match != null) {
                selectedEmployee = match
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Kiosk Badge & Exit Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Translations.get("kiosk_title", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            TextButton(
                onClick = {
                    if (uiState.settings.pinProtectionEnabled) {
                        showPinDialog = true
                    } else {
                        viewModel.setScreen(AppScreen.TODAY)
                    }
                },
                modifier = Modifier.testTag("exit_kiosk_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.get("exit_kiosk", lang))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Digital Clock
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDigits(uiState.currentTime, useArabicIndic),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDigits(uiState.currentDate, useArabicIndic),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Banner
        AnimatedVisibility(visible = uiState.feedbackMessage != null || uiState.errorMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (uiState.errorMessage != null) StatusLateBg else StatusOnTimeBg
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.errorMessage != null) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (uiState.errorMessage != null) StatusLate else StatusOnTime
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.errorMessage ?: uiState.feedbackMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.errorMessage != null) StatusLate else StatusOnTime
                    )
                }
            }
        }

        // Active Selected Employee Display
        val emp = selectedEmployee
        if (emp != null) {
            val record = todayMap[emp.id]
            val isCheckedIn = record?.checkInTime != null
            val isCheckedOut = record?.checkOutTime != null

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emp.name.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = emp.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = "${formatDigits(emp.code, useArabicIndic)} • ${emp.department}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isCheckedIn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${Translations.get("check_in", lang)}: ${formatDigits(record?.checkInTime.orEmpty(), useArabicIndic)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusOnTime
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.checkIn(emp, isKiosk = true)
                                enteredCode = ""
                            },
                            enabled = !isCheckedIn,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOnTime),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("kiosk_check_in_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translations.get("check_in", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.checkOut(emp, isKiosk = true)
                                enteredCode = ""
                            },
                            enabled = isCheckedIn && !isCheckedOut,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("kiosk_check_out_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translations.get("check_out", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        selectedEmployee = null
                        enteredCode = ""
                    }) {
                        Text(Translations.get("cancel", lang))
                    }
                }
            }
        } else {
            // Code Input Field
            OutlinedTextField(
                value = enteredCode,
                onValueChange = { enteredCode = it },
                label = { Text(Translations.get("enter_code", lang)) },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                trailingIcon = {
                    if (enteredCode.isNotEmpty()) {
                        IconButton(onClick = { enteredCode = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kiosk_code_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = Translations.get("quick_tap", lang),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Select Employee Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(uiState.employees.filter { it.isActive }, key = { it.id }) { e ->
                    val record = todayMap[e.id]
                    val isCheckedIn = record?.checkInTime != null
                    val isCheckedOut = record?.checkOutTime != null

                    Card(
                        modifier = Modifier
                            .clickable {
                                selectedEmployee = e
                                enteredCode = e.code
                            }
                            .testTag("kiosk_emp_${e.code}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCheckedOut) StatusAbsentBg else if (isCheckedIn) StatusOnTimeBg else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCheckedIn) StatusOnTime else TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = e.name.take(2).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = e.name.split(" ").take(2).joinToString(" "),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = formatDigits(e.code, useArabicIndic),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            language = lang,
            onVerify = { pin -> viewModel.unlockManager(pin) },
            onSuccess = {
                showPinDialog = false
                viewModel.setScreen(AppScreen.TODAY)
            },
            onDismiss = { showPinDialog = false }
        )
    }
}
