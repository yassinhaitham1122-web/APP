package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.TimeUtils

@Composable
fun PenaltyDialog(
    employeeName: String,
    record: AttendanceRecord,
    currency: String,
    language: AppLanguage,
    useArabicIndic: Boolean,
    onConfirm: (amount: Double, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(record.penaltyAmount.toString()) }
    var reasonText by remember { mutableStateOf("") }
    val latenessStr = TimeUtils.formatSecondsToHms(record.latenessSeconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Translations.get("apply_penalty", language),
                fontWeight = FontWeight.Bold,
                color = StatusLate
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${Translations.get("full_name", language)}: $employeeName",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusLateBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Translations.get("lateness_duration", language)}: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusLate
                        )
                        Text(
                            text = formatDigits(latenessStr, useArabicIndic),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusLate
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("${Translations.get("penalty_amount", language)} ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("penalty_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text(Translations.get("reason_for_penalty", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("penalty_reason_input"),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: record.penaltyAmount
                    onConfirm(amount, reasonText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusLate),
                modifier = Modifier.testTag("confirm_penalty_button")
            ) {
                Text(Translations.get("confirm", language))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_penalty_button")
            ) {
                Text(Translations.get("cancel", language))
            }
        }
    )
}

@Composable
fun ExcuseDialog(
    employeeName: String,
    record: AttendanceRecord,
    language: AppLanguage,
    useArabicIndic: Boolean,
    onConfirm: (excuseType: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val excuseTypes = listOf(
        Pair("excuse_sick", "Sick Leave"),
        Pair("excuse_emergency", "Family Emergency"),
        Pair("excuse_assignment", "Work Assignment"),
        Pair("excuse_prior", "Prior Permission"),
        Pair("excuse_other", "Other")
    )

    var selectedType by remember { mutableStateOf(excuseTypes[0].second) }
    var noteText by remember { mutableStateOf("") }
    val latenessStr = TimeUtils.formatSecondsToHms(record.latenessSeconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Translations.get("accept_excuse", language),
                fontWeight = FontWeight.Bold,
                color = StatusExcused
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${Translations.get("full_name", language)}: $employeeName",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Text(
                    text = "${Translations.get("lateness_duration", language)}: ${formatDigits(latenessStr, useArabicIndic)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = Translations.get("excuse_type", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column {
                    excuseTypes.forEach { (key, typeVal) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedType == typeVal,
                                onClick = { selectedType = typeVal }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Translations.get(key, language),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(Translations.get("excuse_note", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("excuse_note_input"),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedType, noteText) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusExcused),
                modifier = Modifier.testTag("confirm_excuse_button")
            ) {
                Text(Translations.get("confirm", language))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_excuse_button")
            ) {
                Text(Translations.get("cancel", language))
            }
        }
    )
}

@Composable
fun ManualAttendanceDialog(
    employee: Employee,
    record: AttendanceRecord?,
    currentDate: String,
    language: AppLanguage,
    onConfirm: (AttendanceRecord, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var checkIn by remember { mutableStateOf(record?.checkInTime ?: "09:00:00") }
    var checkOut by remember { mutableStateOf(record?.checkOutTime ?: "17:00:00") }
    var selectedStatus by remember { mutableStateOf(record?.status ?: AttendanceStatus.ON_TIME) }
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Translations.get("manual_edit_title", language),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${Translations.get("full_name", language)}: ${employee.name}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                OutlinedTextField(
                    value = checkIn,
                    onValueChange = { checkIn = it },
                    label = { Text("${Translations.get("check_in", language)} (HH:mm:ss)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = checkOut,
                    onValueChange = { checkOut = it },
                    label = { Text("${Translations.get("check_out", language)} (HH:mm:ss)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = Translations.get("manual_reason_prompt", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        errorMessage = null
                    },
                    label = { Text(Translations.get("manual_reason_prompt", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_reason_input"),
                    minLines = 2,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        errorMessage = Translations.get("edit_reason_required", language)
                        return@Button
                    }
                    val updated = (record ?: AttendanceRecord(
                        employeeId = employee.id,
                        date = currentDate
                    )).copy(
                        checkInTime = checkIn.ifBlank { null },
                        checkOutTime = checkOut.ifBlank { null },
                        status = selectedStatus
                    )
                    onConfirm(updated, reason)
                },
                modifier = Modifier.testTag("save_manual_record_button")
            ) {
                Text(Translations.get("save", language))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_manual_record_button")
            ) {
                Text(Translations.get("cancel", language))
            }
        }
    )
}

@Composable
fun EmployeeDialog(
    initialEmployee: Employee?,
    language: AppLanguage,
    currency: String,
    onSave: (Employee) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialEmployee?.name ?: "") }
    var code by remember { mutableStateOf(initialEmployee?.code ?: "") }
    var dept by remember { mutableStateOf(initialEmployee?.department ?: "") }
    var phone by remember { mutableStateOf(initialEmployee?.phone ?: "") }
    var wageText by remember { mutableStateOf(initialEmployee?.dailyWage?.toString() ?: "300") }
    var isActive by remember { mutableStateOf(initialEmployee?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEmployee == null) Translations.get("add_employee", language) else Translations.get("edit_employee", language),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(Translations.get("full_name", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = { Text(Translations.get("employee_code", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_code_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text(Translations.get("job_title_dept", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Translations.get("phone", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = wageText,
                    onValueChange = { wageText = it },
                    label = { Text("${Translations.get("daily_wage", language)} ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) Translations.get("active", language) else Translations.get("inactive", language),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || code.isBlank()) {
                        error = Translations.get("required_field", language)
                        return@Button
                    }
                    val wage = wageText.toDoubleOrNull() ?: 0.0
                    val emp = (initialEmployee ?: Employee(
                        code = code.trim(),
                        name = name.trim(),
                        department = dept.trim()
                    )).copy(
                        name = name.trim(),
                        code = code.trim(),
                        department = dept.trim(),
                        phone = phone.trim(),
                        dailyWage = wage,
                        isActive = isActive
                    )
                    onSave(emp)
                },
                modifier = Modifier.testTag("save_employee_button")
            ) {
                Text(Translations.get("save", language))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_employee_button")
            ) {
                Text(Translations.get("cancel", language))
            }
        }
    )
}

@Composable
fun PinDialog(
    language: AppLanguage,
    onVerify: (pin: String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Translations.get("pin_prompt", language),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_code_input")
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerify(pin)) {
                        onSuccess()
                    } else {
                        error = Translations.get("incorrect_pin", language)
                    }
                },
                modifier = Modifier.testTag("submit_pin_button")
            ) {
                Text(Translations.get("confirm", language))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_pin_button")
            ) {
                Text(Translations.get("cancel", language))
            }
        }
    )
}

@Composable
fun LeaveDialog(
    employees: List<Employee>,
    language: AppLanguage,
    currentDate: String,
    onSave: (LeaveRecord, employeeName: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (employees.isEmpty()) return

    var selectedEmployee by remember { mutableStateOf(employees[0]) }
    var leaveType by remember { mutableStateOf("Annual") }
    var startDate by remember { mutableStateOf(currentDate) }
    var endDate by remember { mutableStateOf(currentDate) }
    var daysText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Translations.get("record_leave", language),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Translations.get("select_employee", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Employee picker chip row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    employees.take(3).forEach { emp ->
                        FilterChip(
                            selected = selectedEmployee.id == emp.id,
                            onClick = { selectedEmployee = emp },
                            label = { Text(emp.name.split(" ").firstOrNull() ?: emp.name) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = leaveType == "Annual",
                        onClick = { leaveType = "Annual" },
                        label = { Text(Translations.get("annual_leave", language)) }
                    )
                    FilterChip(
                        selected = leaveType == "Sick",
                        onClick = { leaveType = "Sick" },
                        label = { Text(Translations.get("sick_leave", language)) }
                    )
                }

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text(Translations.get("start_date", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text(Translations.get("end_date", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text(Translations.get("days_count", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(Translations.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysText.toIntOrNull() ?: 1
                    onSave(
                        LeaveRecord(
                            employeeId = selectedEmployee.id,
                            type = leaveType,
                            startDate = startDate,
                            endDate = endDate,
                            daysCount = days,
                            reason = reason
                        ),
                        selectedEmployee.name
                    )
                }
            ) {
                Text(Translations.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Translations.get("cancel", language))
            }
        }
    )
}
