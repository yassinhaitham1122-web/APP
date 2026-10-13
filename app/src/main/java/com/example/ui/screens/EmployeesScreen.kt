package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.Employee
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateWidget
import com.example.ui.dialogs.EmployeeDialog
import com.example.ui.theme.*

@Composable
fun EmployeesScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val lang = uiState.language
    val useArabicIndic = uiState.settings.useArabicIndicDigits
    val currency = uiState.settings.currency

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var deletingEmployee by remember { mutableStateOf<Employee?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Active, false = Inactive

    val filtered = remember(uiState.employees, searchQuery, activeFilter) {
        uiState.employees.filter { emp ->
            val matchesQuery = searchQuery.isBlank() ||
                    emp.name.contains(searchQuery, ignoreCase = true) ||
                    emp.code.contains(searchQuery, ignoreCase = true) ||
                    emp.department.contains(searchQuery, ignoreCase = true) ||
                    emp.phone.contains(searchQuery)

            val matchesStatus = activeFilter == null || emp.isActive == activeFilter
            matchesQuery && matchesStatus
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_employee_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = Translations.get("add_employee", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val total = uiState.employees.size
                    val active = uiState.employees.count { it.isActive }
                    val inactive = total - active

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(Translations.get("all", lang), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatDigits(total.toString(), useArabicIndic),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = StatusOnTimeBg)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(Translations.get("active", lang), style = MaterialTheme.typography.labelSmall, color = StatusOnTime)
                            Text(
                                text = formatDigits(active.toString(), useArabicIndic),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = StatusOnTime
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = StatusAbsentBg)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(Translations.get("inactive", lang), style = MaterialTheme.typography.labelSmall, color = StatusAbsent)
                            Text(
                                text = formatDigits(inactive.toString(), useArabicIndic),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = StatusAbsent
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Translations.get("search", lang)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_management_search"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { activeFilter = null },
                        label = { Text(Translations.get("all", lang)) }
                    )
                    FilterChip(
                        selected = activeFilter == true,
                        onClick = { activeFilter = true },
                        label = { Text(Translations.get("active", lang)) }
                    )
                    FilterChip(
                        selected = activeFilter == false,
                        onClick = { activeFilter = false },
                        label = { Text(Translations.get("inactive", lang)) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateWidget(
                        title = Translations.get("no_records_found", lang),
                        subtitle = Translations.get("search", lang),
                        icon = Icons.Default.PersonOff
                    )
                }
            } else {
                items(filtered, key = { it.id }) { employee ->
                    EmployeeDetailCard(
                        employee = employee,
                        language = lang,
                        currency = currency,
                        useArabicIndic = useArabicIndic,
                        onEdit = { editingEmployee = employee },
                        onDelete = { deletingEmployee = employee },
                        onToggleActive = { viewModel.toggleEmployeeStatus(employee) }
                    )
                }
            }
        }
    }

    // Add or Edit Employee Dialog
    if (showAddDialog || editingEmployee != null) {
        EmployeeDialog(
            initialEmployee = editingEmployee,
            language = lang,
            currency = currency,
            onSave = { emp ->
                viewModel.saveEmployee(emp)
                showAddDialog = false
                editingEmployee = null
            },
            onDismiss = {
                showAddDialog = false
                editingEmployee = null
            }
        )
    }

    // Delete Confirmation Dialog
    deletingEmployee?.let { emp ->
        AlertDialog(
            onDismissRequest = { deletingEmployee = null },
            title = {
                Text(
                    text = Translations.get("delete_confirm", lang),
                    fontWeight = FontWeight.Bold,
                    color = StatusLate
                )
            },
            text = {
                Text(
                    text = "${emp.name} (${emp.code})\n${Translations.get("action_cannot_be_undone", lang)}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee(emp)
                        deletingEmployee = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLate),
                    modifier = Modifier.testTag("confirm_delete_employee")
                ) {
                    Text(Translations.get("delete", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEmployee = null }) {
                    Text(Translations.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun EmployeeDetailCard(
    employee: Employee,
    language: AppLanguage,
    currency: String,
    useArabicIndic: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("emp_detail_card_${employee.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (employee.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (employee.isActive) TealPrimary else StatusAbsent),
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
                    Text(
                        text = employee.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${formatDigits(employee.code, useArabicIndic)} • ${employee.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (employee.isActive) StatusOnTimeBg else StatusAbsentBg
                ) {
                    Text(
                        text = if (employee.isActive) Translations.get("active", language) else Translations.get("inactive", language),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (employee.isActive) StatusOnTime else StatusAbsent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Info Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (employee.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDigits(employee.phone, useArabicIndic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${Translations.get("daily_wage", language)}: ${formatDigits(employee.dailyWage.toString(), useArabicIndic)} $currency",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Leave Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${Translations.get("annual_leave", language)}: ${formatDigits(employee.annualLeaveBalance.toString(), useArabicIndic)} ${Translations.get("days", language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealPrimary
                )
                Text(
                    text = "${Translations.get("sick_leave", language)}: ${formatDigits(employee.sickLeaveBalance.toString(), useArabicIndic)} ${Translations.get("days", language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = IndigoSecondary
                )
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleActive) {
                    Text(
                        text = if (employee.isActive) Translations.get("deactivate", language) else Translations.get("activate", language),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = Translations.get("edit", language), modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = Translations.get("delete", language), tint = StatusLate, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
