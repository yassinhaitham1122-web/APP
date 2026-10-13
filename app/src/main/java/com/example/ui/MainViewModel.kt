package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.model.*
import com.example.util.SoundHelper
import com.example.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppScreen {
    TODAY,
    KIOSK,
    EMPLOYEES,
    REPORTS,
    LEAVES,
    SETTINGS
}

data class EmployeeWithTodayAttendance(
    val employee: Employee,
    val record: AttendanceRecord?,
    val isDayOff: Boolean = false,
    val isHoliday: Boolean = false,
    val holidayName: String? = null
)

data class MainUiState(
    val employees: List<Employee> = emptyList(),
    val todayRecords: List<AttendanceRecord> = emptyList(),
    val allRecords: List<AttendanceRecord> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
    val leaves: List<LeaveRecord> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val language: AppLanguage = AppLanguage.AR,
    val isDarkMode: Boolean = false,
    val currentScreen: AppScreen = AppScreen.TODAY,
    val isManagerUnlocked: Boolean = true,
    val currentTime: String = "00:00:00",
    val currentDate: String = "2026-10-13",
    val searchQuery: String = "",
    val filterStatus: AttendanceStatus? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val missingCheckOutCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AttendanceRepository(db)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null

    init {
        startLiveClock()
        observeData()
    }

    private fun startLiveClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                val tz = _uiState.value.settings.timezone
                val time = TimeUtils.getCurrentTime(tz)
                val date = TimeUtils.getCurrentDate(tz)
                _uiState.update { it.copy(currentTime = time, currentDate = date) }
                delay(1000)
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.allEmployees.collect { list ->
                _uiState.update { it.copy(employees = list) }
            }
        }

        viewModelScope.launch {
            repository.allHolidays.collect { list ->
                _uiState.update { it.copy(holidays = list) }
            }
        }

        viewModelScope.launch {
            repository.allLeaves.collect { list ->
                _uiState.update { it.copy(leaves = list) }
            }
        }

        viewModelScope.launch {
            repository.allAuditLogs.collect { list ->
                _uiState.update { it.copy(auditLogs = list) }
            }
        }

        viewModelScope.launch {
            repository.getAllRecords().collect { list ->
                val today = _uiState.value.currentDate
                val todayList = list.filter { it.date == today }
                val missing = list.count { it.date < today && it.checkInTime != null && (it.checkOutTime == null || it.checkOutTime.isEmpty()) }
                _uiState.update {
                    it.copy(
                        allRecords = list,
                        todayRecords = todayList,
                        missingCheckOutCount = missing
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.settingsFlow.collect { s ->
                val settings = s ?: AppSettings()
                _uiState.update {
                    it.copy(
                        settings = settings,
                        language = if (settings.isArabic) AppLanguage.AR else AppLanguage.EN,
                        isDarkMode = settings.isDarkMode,
                        isManagerUnlocked = !settings.pinProtectionEnabled
                    )
                }
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        if (_uiState.value.settings.pinProtectionEnabled &&
            screen != AppScreen.KIOSK &&
            !_uiState.value.isManagerUnlocked
        ) {
            // Prompt for PIN first
            return
        }
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun unlockManager(pin: String): Boolean {
        return if (pin == _uiState.value.settings.managerPin) {
            _uiState.update { it.copy(isManagerUnlocked = true) }
            true
        } else {
            false
        }
    }

    fun lockManager() {
        if (_uiState.value.settings.pinProtectionEnabled) {
            _uiState.update {
                it.copy(
                    isManagerUnlocked = false,
                    currentScreen = AppScreen.KIOSK
                )
            }
        }
    }

    fun toggleLanguage() {
        val newLang = if (_uiState.value.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
        val isArabic = (newLang == AppLanguage.AR)
        val newSettings = _uiState.value.settings.copy(isArabic = isArabic)
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            _uiState.update { it.copy(language = newLang) }
        }
    }

    fun toggleDarkMode() {
        val newMode = !_uiState.value.isDarkMode
        val newSettings = _uiState.value.settings.copy(isDarkMode = newMode)
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            _uiState.update { it.copy(isDarkMode = newMode) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setStatusFilter(status: AttendanceStatus?) {
        _uiState.update { it.copy(filterStatus = status) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }

    fun checkIn(employee: Employee, isKiosk: Boolean = false) {
        viewModelScope.launch {
            val tz = _uiState.value.settings.timezone
            val today = TimeUtils.getCurrentDate(tz)
            val time = TimeUtils.getCurrentTime(tz)

            val result = repository.checkIn(
                employeeId = employee.id,
                employeeName = employee.name,
                date = today,
                currentTime = time,
                performedBy = if (isKiosk) "Kiosk (${employee.code})" else "Manager"
            )

            if (result.isSuccess) {
                SoundHelper.playCheckInSound(getApplication())
                val msg = Translations.get("success_check_in", _uiState.value.language)
                _uiState.update { it.copy(feedbackMessage = "${employee.name}: $msg ($time)") }
            } else {
                SoundHelper.playWarningSound(getApplication())
                val errKey = when (result.exceptionOrNull()?.message) {
                    "ALREADY_CHECKED_IN" -> "already_checked_in"
                    else -> "cannot_checkout_before_checkin"
                }
                val msg = Translations.get(errKey, _uiState.value.language)
                _uiState.update { it.copy(errorMessage = "${employee.name}: $msg") }
            }
        }
    }

    fun checkOut(employee: Employee, isKiosk: Boolean = false) {
        viewModelScope.launch {
            val tz = _uiState.value.settings.timezone
            val today = TimeUtils.getCurrentDate(tz)
            val time = TimeUtils.getCurrentTime(tz)

            val result = repository.checkOut(
                employeeId = employee.id,
                employeeName = employee.name,
                date = today,
                currentTime = time,
                performedBy = if (isKiosk) "Kiosk (${employee.code})" else "Manager"
            )

            if (result.isSuccess) {
                SoundHelper.playCheckOutSound(getApplication())
                val msg = Translations.get("success_check_out", _uiState.value.language)
                _uiState.update { it.copy(feedbackMessage = "${employee.name}: $msg ($time)") }
            } else {
                SoundHelper.playWarningSound(getApplication())
                val errKey = when (result.exceptionOrNull()?.message) {
                    "ALREADY_CHECKED_OUT" -> "already_checked_out"
                    else -> "cannot_checkout_before_checkin"
                }
                val msg = Translations.get(errKey, _uiState.value.language)
                _uiState.update { it.copy(errorMessage = "${employee.name}: $msg") }
            }
        }
    }

    fun applyPenalty(
        recordId: Long,
        employeeName: String,
        amount: Double,
        reason: String,
        approvedBy: String = "Manager"
    ) {
        viewModelScope.launch {
            repository.applyPenalty(recordId, employeeName, amount, reason, approvedBy)
            _uiState.update {
                it.copy(feedbackMessage = Translations.get("penalty_applied_msg", _uiState.value.language))
            }
        }
    }

    fun acceptExcuse(
        recordId: Long,
        employeeName: String,
        excuseType: String,
        note: String,
        approvedBy: String = "Manager"
    ) {
        viewModelScope.launch {
            repository.acceptExcuse(recordId, employeeName, excuseType, note, approvedBy)
            _uiState.update {
                it.copy(feedbackMessage = Translations.get("excuse_accepted_msg", _uiState.value.language))
            }
        }
    }

    fun saveManualAttendance(
        record: AttendanceRecord,
        employeeName: String,
        reason: String
    ) {
        viewModelScope.launch {
            repository.saveManualRecord(record, employeeName, reason, "Manager")
            _uiState.update {
                it.copy(feedbackMessage = Translations.get("save", _uiState.value.language) + " " + employeeName)
            }
        }
    }

    fun saveEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.saveEmployee(employee)
            _uiState.update {
                it.copy(feedbackMessage = "${employee.name} saved")
            }
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            _uiState.update {
                it.copy(feedbackMessage = "${employee.name} deleted")
            }
        }
    }

    fun toggleEmployeeStatus(employee: Employee) {
        viewModelScope.launch {
            repository.toggleEmployeeActive(employee)
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            startLiveClock() // Rebind timezone if changed
            _uiState.update {
                it.copy(
                    settings = settings,
                    language = if (settings.isArabic) AppLanguage.AR else AppLanguage.EN,
                    isDarkMode = settings.isDarkMode
                )
            }
        }
    }

    fun addHoliday(date: String, name: String) {
        viewModelScope.launch {
            repository.addHoliday(Holiday(date = date, name = name))
        }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch {
            repository.deleteHoliday(id)
        }
    }

    fun addLeave(leave: LeaveRecord, employeeName: String) {
        viewModelScope.launch {
            repository.addLeave(leave, employeeName)
            _uiState.update {
                it.copy(feedbackMessage = "Leave recorded for $employeeName")
            }
        }
    }

    fun deleteLeave(id: Long) {
        viewModelScope.launch {
            repository.deleteLeave(id)
        }
    }

    suspend fun exportJson(): String {
        return repository.exportToJson()
    }

    fun importJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.importFromJson(json)
            if (res.isSuccess) {
                onResult(true, "Database imported successfully")
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Import failed")
            }
        }
    }

    fun generateCsv(
        records: List<AttendanceRecord>,
        employees: Map<Long, Employee>,
        language: AppLanguage,
        currency: String
    ): String {
        val isAr = language == AppLanguage.AR
        val headers = if (isAr) {
            "التاريخ,كود الموظف,اسم الموظف,القسم,وقت الحضور,وقت الانصراف,الحالة,مدة التأخير,الخصم ($currency),حالة الخصم,نوع العذر,ملاحظات"
        } else {
            "Date,Employee Code,Employee Name,Department,Check In,Check Out,Status,Lateness,Penalty ($currency),Penalty Status,Excuse,Notes"
        }

        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM for Excel Arabic encoding
        sb.append(headers).append("\n")

        for (r in records) {
            val emp = employees[r.employeeId]
            val empName = emp?.name?.replace(",", " ") ?: "Unknown"
            val empCode = emp?.code ?: ""
            val dept = emp?.department?.replace(",", " ") ?: ""
            val checkIn = r.checkInTime ?: ""
            val checkOut = r.checkOutTime ?: ""
            val statusStr = Translations.get("status_" + r.status.name.lowercase(), language)
            val latenessStr = TimeUtils.formatSecondsToHms(r.latenessSeconds)
            val penalty = r.penaltyAmount.toString()
            val penaltyStatus = r.penaltyStatus.name
            val excuse = (r.excuseType ?: "").replace(",", " ")
            val note = (r.note ?: r.manualEditReason ?: "").replace(",", " ")

            sb.append("\"${r.date}\",")
                .append("\"$empCode\",")
                .append("\"$empName\",")
                .append("\"$dept\",")
                .append("\"$checkIn\",")
                .append("\"$checkOut\",")
                .append("\"$statusStr\",")
                .append("\"$latenessStr\",")
                .append("\"$penalty\",")
                .append("\"$penaltyStatus\",")
                .append("\"$excuse\",")
                .append("\"$note\"\n")
        }
        return sb.toString()
    }
}
