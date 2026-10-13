package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateWidget
import com.example.ui.dialogs.LeaveDialog
import com.example.ui.theme.*

@Composable
fun LeavesScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val lang = uiState.language
    val useArabicIndic = uiState.settings.useArabicIndicDigits
    var showRecordDialog by remember { mutableStateOf(false) }

    val empMap = remember(uiState.employees) { uiState.employees.associateBy { it.id } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRecordDialog = true },
                containerColor = IndigoSecondary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.testTag("record_leave_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = Translations.get("record_leave", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = Translations.get("leaves_title", lang),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Employee Balance Summary Cards
            item {
                Text(
                    text = "${Translations.get("remaining_days", lang)} (${Translations.get("employees", lang)})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(uiState.employees.filter { it.isActive }, key = { it.id }) { emp ->
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TealPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = Translations.get("annual_leave", lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TealPrimary
                                    )
                                    Text(
                                        text = "${formatDigits(emp.annualLeaveBalance.toString(), useArabicIndic)} ${Translations.get("days", lang)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TealPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = IndigoSecondary.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = Translations.get("sick_leave", lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IndigoSecondary
                                    )
                                    Text(
                                        text = "${formatDigits(emp.sickLeaveBalance.toString(), useArabicIndic)} ${Translations.get("days", lang)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = IndigoSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Leave History Log
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Translations.get("audit_logs", lang),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.leaves.isEmpty()) {
                item {
                    EmptyStateWidget(
                        title = Translations.get("no_records_found", lang),
                        subtitle = Translations.get("record_leave", lang),
                        icon = Icons.Default.BeachAccess
                    )
                }
            } else {
                items(uiState.leaves, key = { it.id }) { leave ->
                    val emp = empMap[leave.employeeId]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = emp?.name ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${leave.type} • ${formatDigits(leave.startDate, useArabicIndic)} -> ${formatDigits(leave.endDate, useArabicIndic)} (${formatDigits(leave.daysCount.toString(), useArabicIndic)} ${Translations.get("days", lang)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (leave.reason.isNotBlank()) {
                                    Text(
                                        text = leave.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deleteLeave(leave.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = Translations.get("delete", lang), tint = StatusLate, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        LeaveDialog(
            employees = uiState.employees.filter { it.isActive },
            language = lang,
            currentDate = uiState.currentDate,
            onSave = { leaveRecord, empName ->
                viewModel.addLeave(leaveRecord, empName)
                showRecordDialog = false
            },
            onDismiss = { showRecordDialog = false }
        )
    }
}
