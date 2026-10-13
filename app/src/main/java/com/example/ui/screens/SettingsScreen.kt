package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.model.AppSettings
import com.example.model.PenaltyRuleType
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val lang = uiState.language
    val useArabicIndic = uiState.settings.useArabicIndicDigits

    var startTime by remember(uiState.settings) { mutableStateOf(uiState.settings.workStartTime) }
    var endTime by remember(uiState.settings) { mutableStateOf(uiState.settings.workEndTime) }
    var gracePeriod by remember(uiState.settings) { mutableStateOf(uiState.settings.gracePeriodMinutes.toString()) }
    var penaltyRule by remember(uiState.settings) { mutableStateOf(uiState.settings.penaltyRuleType) }
    var perMinuteRate by remember(uiState.settings) { mutableStateOf(uiState.settings.perMinuteRate.toString()) }
    var tier1 by remember(uiState.settings) { mutableStateOf(uiState.settings.tieredUpTo15Min.toString()) }
    var tier2 by remember(uiState.settings) { mutableStateOf(uiState.settings.tiered15To60Min.toString()) }
    var fixedAmount by remember(uiState.settings) { mutableStateOf(uiState.settings.fixedPenaltyAmount.toString()) }
    var currency by remember(uiState.settings) { mutableStateOf(uiState.settings.currency) }
    var timezone by remember(uiState.settings) { mutableStateOf(uiState.settings.timezone) }
    var useArabicDigits by remember(uiState.settings) { mutableStateOf(uiState.settings.useArabicIndicDigits) }
    var pinProtection by remember(uiState.settings) { mutableStateOf(uiState.settings.pinProtectionEnabled) }
    var managerPin by remember(uiState.settings) { mutableStateOf(uiState.settings.managerPin) }

    // Backup & Restore Dialog state
    var showBackupDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }

    // Holiday Dialog state
    var showHolidayDialog by remember { mutableStateOf(false) }
    var holidayName by remember { mutableStateOf("") }
    var holidayDate by remember { mutableStateOf(uiState.currentDate) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Translations.get("settings_title", lang),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // 1. Work Schedule Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Translations.get("work_schedule", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text(Translations.get("work_start_time", lang)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_time_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text(Translations.get("work_end_time", lang)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("end_time_input"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = gracePeriod,
                        onValueChange = { gracePeriod = it },
                        label = { Text(Translations.get("grace_period", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grace_period_input"),
                        singleLine = true
                    )
                }
            }
        }

        // 2. Penalty Rules Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Translations.get("penalty_settings", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StatusLate
                    )

                    // Rule A: Per-Minute
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = penaltyRule == PenaltyRuleType.PER_MINUTE,
                            onClick = { penaltyRule = PenaltyRuleType.PER_MINUTE }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("penalty_per_minute", lang), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (penaltyRule == PenaltyRuleType.PER_MINUTE) {
                        OutlinedTextField(
                            value = perMinuteRate,
                            onValueChange = { perMinuteRate = it },
                            label = { Text("${Translations.get("rate_per_minute", lang)} ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true
                        )
                    }

                    // Rule B: Tiered
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = penaltyRule == PenaltyRuleType.TIERED,
                            onClick = { penaltyRule = PenaltyRuleType.TIERED }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("penalty_tiered", lang), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (penaltyRule == PenaltyRuleType.TIERED) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tier1,
                                onValueChange = { tier1 = it },
                                label = { Text("${Translations.get("tier_1_rate", lang)} ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tier2,
                                onValueChange = { tier2 = it },
                                label = { Text("${Translations.get("tier_2_rate", lang)} ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = Translations.get("tier_3_rate", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Rule C: Fixed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = penaltyRule == PenaltyRuleType.FIXED,
                            onClick = { penaltyRule = PenaltyRuleType.FIXED }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("penalty_fixed", lang), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (penaltyRule == PenaltyRuleType.FIXED) {
                        OutlinedTextField(
                            value = fixedAmount,
                            onValueChange = { fixedAmount = it },
                            label = { Text("${Translations.get("fixed_rate", lang)} ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 3. Currency, Timezone & Digits
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${Translations.get("currency_setting", lang)} & ${Translations.get("timezone_setting", lang)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = IndigoSecondary
                    )

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text(Translations.get("currency_setting", lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = timezone,
                        onValueChange = { timezone = it },
                        label = { Text(Translations.get("timezone_setting", lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timezone_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (useArabicDigits) Translations.get("arabic_indic_digits", lang) else Translations.get("western_digits", lang),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = useArabicDigits,
                            onCheckedChange = { useArabicDigits = it }
                        )
                    }
                }
            }
        }

        // 4. Manager PIN Lock Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translations.get("manager_pin_setting", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Switch(
                            checked = pinProtection,
                            onCheckedChange = { pinProtection = it }
                        )
                    }

                    if (pinProtection) {
                        OutlinedTextField(
                            value = managerPin,
                            onValueChange = { if (it.length <= 6) managerPin = it },
                            label = { Text(Translations.get("change_pin", lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manager_pin_input"),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Save Settings Action Button
        item {
            Button(
                onClick = {
                    val updated = uiState.settings.copy(
                        workStartTime = startTime.trim(),
                        workEndTime = endTime.trim(),
                        gracePeriodMinutes = gracePeriod.toIntOrNull() ?: 10,
                        penaltyRuleType = penaltyRule,
                        perMinuteRate = perMinuteRate.toDoubleOrNull() ?: 1.0,
                        tieredUpTo15Min = tier1.toDoubleOrNull() ?: 10.0,
                        tiered15To60Min = tier2.toDoubleOrNull() ?: 30.0,
                        fixedPenaltyAmount = fixedAmount.toDoubleOrNull() ?: 50.0,
                        currency = currency.trim(),
                        timezone = timezone.trim(),
                        useArabicIndicDigits = useArabicDigits,
                        pinProtectionEnabled = pinProtection,
                        managerPin = managerPin.trim()
                    )
                    viewModel.saveSettings(updated)
                    Toast.makeText(context, Translations.get("save", lang), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Translations.get("save", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 5. Official Holidays Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translations.get("official_holidays", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showHolidayDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = Translations.get("add_holiday", lang), tint = TealPrimary)
                        }
                    }

                    uiState.holidays.forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(h.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(formatDigits(h.date, useArabicIndic), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteHoliday(h.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusLate, modifier = Modifier.size(20.dp))
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // 6. JSON Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Translations.get("backup_restore", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val json = viewModel.exportJson()
                                    exportedJsonText = json
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "JSON Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    showBackupDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("export_json", lang), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                showBackupDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("import_json", lang), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Holiday Dialog
    if (showHolidayDialog) {
        AlertDialog(
            onDismissRequest = { showHolidayDialog = false },
            title = { Text(Translations.get("add_holiday", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = holidayName,
                        onValueChange = { holidayName = it },
                        label = { Text(Translations.get("holiday_name", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = holidayDate,
                        onValueChange = { holidayDate = it },
                        label = { Text("${Translations.get("date", lang)} (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (holidayName.isNotBlank() && holidayDate.isNotBlank()) {
                        viewModel.addHoliday(holidayDate, holidayName)
                        showHolidayDialog = false
                        holidayName = ""
                    }
                }) {
                    Text(Translations.get("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHolidayDialog = false }) {
                    Text(Translations.get("cancel", lang))
                }
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(Translations.get("backup_restore", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste JSON data below to restore, or copy exported JSON:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = if (importJsonInput.isNotBlank()) importJsonInput else exportedJsonText,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        label = { Text("JSON Data") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importJsonInput.isNotBlank()) {
                        viewModel.importJson(importJsonInput) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) showBackupDialog = false
                        }
                    } else {
                        showBackupDialog = false
                    }
                }) {
                    Text(if (importJsonInput.isNotBlank()) Translations.get("import_json", lang) else Translations.get("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(Translations.get("cancel", lang))
                }
            }
        )
    }
}
