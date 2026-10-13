package com.example.data

import com.example.model.*
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class AttendanceRepository(private val db: AppDatabase) {
    val employeeDao = db.employeeDao()
    val attendanceDao = db.attendanceDao()
    val holidayDao = db.holidayDao()
    val leaveDao = db.leaveDao()
    val auditLogDao = db.auditLogDao()
    val settingsDao = db.settingsDao()

    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()
    val activeEmployees: Flow<List<Employee>> = employeeDao.getActiveEmployees()
    val allHolidays: Flow<List<Holiday>> = holidayDao.getAllHolidays()
    val allLeaves: Flow<List<LeaveRecord>> = leaveDao.getAllLeaves()
    val allAuditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()
    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettings()

    fun getRecordsByDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecordsByDate(date)

    fun getAllRecords(): Flow<List<AttendanceRecord>> =
        attendanceDao.getAllRecords()

    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecordsInRange(startDate, endDate)

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettingsSync() ?: AppSettings().also {
            settingsDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
        auditLogDao.insertLog(
            AuditLog(
                action = "Update Settings",
                employeeName = "System",
                details = "Updated work schedule or penalty configuration",
                performedBy = "Manager"
            )
        )
    }

    suspend fun saveEmployee(employee: Employee): Long {
        val isNew = employee.id == 0L
        val id = employeeDao.insertEmployee(employee)
        auditLogDao.insertLog(
            AuditLog(
                action = if (isNew) "Add Employee" else "Edit Employee",
                employeeName = employee.name,
                details = "Code: ${employee.code}, Dept: ${employee.department}, Wage: ${employee.dailyWage}",
                performedBy = "Manager"
            )
        )
        return id
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee)
        auditLogDao.insertLog(
            AuditLog(
                action = "Delete Employee",
                employeeName = employee.name,
                details = "Deleted employee record code ${employee.code}",
                performedBy = "Manager"
            )
        )
    }

    suspend fun toggleEmployeeActive(employee: Employee) {
        val updated = employee.copy(isActive = !employee.isActive)
        employeeDao.updateEmployee(updated)
        auditLogDao.insertLog(
            AuditLog(
                action = if (updated.isActive) "Activate Employee" else "Deactivate Employee",
                employeeName = employee.name,
                details = "Status changed to ${if (updated.isActive) "Active" else "Inactive"}",
                performedBy = "Manager"
            )
        )
    }

    suspend fun checkIn(
        employeeId: Long,
        employeeName: String,
        date: String,
        currentTime: String,
        performedBy: String = "Self"
    ): Result<AttendanceRecord> {
        val existing = attendanceDao.getRecord(employeeId, date)
        if (existing?.checkInTime != null) {
            return Result.failure(IllegalStateException("ALREADY_CHECKED_IN"))
        }

        val settings = getSettings()
        val latenessSec = TimeUtils.calculateLatenessSeconds(
            actualCheckIn = currentTime,
            officialStart = settings.workStartTime,
            graceMinutes = settings.gracePeriodMinutes
        )

        val status = if (latenessSec > 0) AttendanceStatus.LATE else AttendanceStatus.ON_TIME
        val suggestedPenalty = if (latenessSec > 0) calculateSuggestedPenalty(latenessSec, settings, employeeId) else 0.0

        val record = (existing ?: AttendanceRecord(
            employeeId = employeeId,
            date = date
        )).copy(
            checkInTime = currentTime,
            status = status,
            latenessSeconds = latenessSec,
            penaltyAmount = suggestedPenalty,
            penaltyStatus = if (latenessSec > 0) PenaltyStatus.PENDING else PenaltyStatus.NONE,
            lastModified = System.currentTimeMillis()
        )

        attendanceDao.insertRecord(record)
        auditLogDao.insertLog(
            AuditLog(
                action = "Check In",
                employeeName = employeeName,
                details = "Checked in at $currentTime. Status: $status, Lateness: ${TimeUtils.formatSecondsToHms(latenessSec)}",
                performedBy = performedBy
            )
        )

        return Result.success(record)
    }

    suspend fun checkOut(
        employeeId: Long,
        employeeName: String,
        date: String,
        currentTime: String,
        performedBy: String = "Self"
    ): Result<AttendanceRecord> {
        val existing = attendanceDao.getRecord(employeeId, date)
        if (existing == null || existing.checkInTime == null) {
            return Result.failure(IllegalStateException("CANNOT_CHECKOUT_BEFORE_CHECKIN"))
        }
        if (existing.checkOutTime != null) {
            return Result.failure(IllegalStateException("ALREADY_CHECKED_OUT"))
        }

        val settings = getSettings()
        val earlySec = TimeUtils.calculateEarlyDepartureSeconds(currentTime, settings.workEndTime)
        val overtimeSec = TimeUtils.calculateOvertimeSeconds(currentTime, settings.workEndTime)

        val record = existing.copy(
            checkOutTime = currentTime,
            earlyDepartureSeconds = earlySec,
            overtimeSeconds = overtimeSec,
            lastModified = System.currentTimeMillis()
        )

        attendanceDao.updateRecord(record)
        auditLogDao.insertLog(
            AuditLog(
                action = "Check Out",
                employeeName = employeeName,
                details = "Checked out at $currentTime. Early departure: ${TimeUtils.formatSecondsToHms(earlySec)}, Overtime: ${TimeUtils.formatSecondsToHms(overtimeSec)}",
                performedBy = performedBy
            )
        )

        return Result.success(record)
    }

    suspend fun calculateSuggestedPenalty(
        latenessSec: Int,
        settings: AppSettings,
        employeeId: Long
    ): Double {
        val latenessMin = latenessSec / 60
        return when (settings.penaltyRuleType) {
            PenaltyRuleType.PER_MINUTE -> {
                latenessMin * settings.perMinuteRate
            }
            PenaltyRuleType.TIERED -> {
                when {
                    latenessMin <= 15 -> settings.tieredUpTo15Min
                    latenessMin <= 60 -> settings.tiered15To60Min
                    else -> {
                        val employee = employeeDao.getEmployeeById(employeeId)
                        val dailyWage = employee?.dailyWage ?: 100.0
                        dailyWage * settings.tieredOver60MinDeductionFraction
                    }
                }
            }
            PenaltyRuleType.FIXED -> {
                settings.fixedPenaltyAmount
            }
        }
    }

    suspend fun applyPenalty(
        recordId: Long,
        employeeName: String,
        penaltyAmount: Double,
        reason: String,
        approvedBy: String
    ) {
        val record = attendanceDao.getAllRecordsSync().find { it.id == recordId } ?: return
        val updated = record.copy(
            penaltyAmount = penaltyAmount,
            penaltyStatus = PenaltyStatus.APPLIED,
            note = reason,
            approvedBy = approvedBy,
            lastModified = System.currentTimeMillis()
        )
        attendanceDao.updateRecord(updated)
        auditLogDao.insertLog(
            AuditLog(
                action = "Apply Penalty",
                employeeName = employeeName,
                details = "Applied penalty of $penaltyAmount. Reason: $reason. Approved by: $approvedBy",
                performedBy = approvedBy
            )
        )
    }

    suspend fun acceptExcuse(
        recordId: Long,
        employeeName: String,
        excuseType: String,
        excuseNote: String,
        approvedBy: String
    ) {
        val record = attendanceDao.getAllRecordsSync().find { it.id == recordId } ?: return
        val updated = record.copy(
            status = AttendanceStatus.EXCUSED,
            penaltyStatus = PenaltyStatus.EXCUSED,
            penaltyAmount = 0.0,
            excuseType = excuseType,
            excuseNote = excuseNote,
            approvedBy = approvedBy,
            lastModified = System.currentTimeMillis()
        )
        attendanceDao.updateRecord(updated)
        auditLogDao.insertLog(
            AuditLog(
                action = "Accept Excuse",
                employeeName = employeeName,
                details = "Accepted excuse [$excuseType]: $excuseNote. Approved by: $approvedBy",
                performedBy = approvedBy
            )
        )
    }

    suspend fun saveManualRecord(
        record: AttendanceRecord,
        employeeName: String,
        reason: String,
        managerName: String
    ) {
        val updated = record.copy(
            manualEditReason = reason,
            approvedBy = managerName,
            lastModified = System.currentTimeMillis()
        )
        attendanceDao.insertRecord(updated)
        auditLogDao.insertLog(
            AuditLog(
                action = "Manual Attendance Edit",
                employeeName = employeeName,
                details = "Date: ${record.date}, In: ${record.checkInTime}, Out: ${record.checkOutTime}, Reason: $reason",
                performedBy = managerName
            )
        )
    }

    suspend fun addHoliday(holiday: Holiday) {
        holidayDao.insertHoliday(holiday)
        auditLogDao.insertLog(
            AuditLog(
                action = "Add Holiday",
                employeeName = "System",
                details = "Holiday: ${holiday.name} on ${holiday.date}",
                performedBy = "Manager"
            )
        )
    }

    suspend fun deleteHoliday(id: Long) {
        holidayDao.deleteHolidayById(id)
        auditLogDao.insertLog(
            AuditLog(
                action = "Delete Holiday",
                employeeName = "System",
                details = "Deleted holiday ID $id",
                performedBy = "Manager"
            )
        )
    }

    suspend fun addLeave(leave: LeaveRecord, employeeName: String) {
        leaveDao.insertLeave(leave)
        val emp = employeeDao.getEmployeeById(leave.employeeId)
        if (emp != null) {
            if (leave.type == "Annual") {
                val newBal = (emp.annualLeaveBalance - leave.daysCount).coerceAtLeast(0)
                employeeDao.updateLeaveBalances(emp.id, newBal, emp.sickLeaveBalance)
            } else if (leave.type == "Sick") {
                val newBal = (emp.sickLeaveBalance - leave.daysCount).coerceAtLeast(0)
                employeeDao.updateLeaveBalances(emp.id, emp.annualLeaveBalance, newBal)
            }
        }
        auditLogDao.insertLog(
            AuditLog(
                action = "Record Leave",
                employeeName = employeeName,
                details = "${leave.type} leave for ${leave.daysCount} days (${leave.startDate} to ${leave.endDate})",
                performedBy = "Manager"
            )
        )
    }

    suspend fun deleteLeave(id: Long) {
        leaveDao.deleteLeaveById(id)
    }

    suspend fun checkMissingCheckOuts(todayDate: String): List<AttendanceRecord> {
        return attendanceDao.getMissingCheckOutRecords(todayDate)
    }

    suspend fun exportToJson(): String {
        val root = JSONObject()
        val emps = employeeDao.getAllEmployeesSync()
        val empArray = JSONArray()
        emps.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("code", e.code)
            obj.put("name", e.name)
            obj.put("department", e.department)
            obj.put("phone", e.phone)
            obj.put("dailyWage", e.dailyWage)
            obj.put("isActive", e.isActive)
            obj.put("annualLeaveBalance", e.annualLeaveBalance)
            obj.put("sickLeaveBalance", e.sickLeaveBalance)
            empArray.put(obj)
        }
        root.put("employees", empArray)

        val records = attendanceDao.getAllRecordsSync()
        val recArray = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("employeeId", r.employeeId)
            obj.put("date", r.date)
            obj.put("checkInTime", r.checkInTime ?: "")
            obj.put("checkOutTime", r.checkOutTime ?: "")
            obj.put("status", r.status.name)
            obj.put("latenessSeconds", r.latenessSeconds)
            obj.put("earlyDepartureSeconds", r.earlyDepartureSeconds)
            obj.put("overtimeSeconds", r.overtimeSeconds)
            obj.put("penaltyAmount", r.penaltyAmount)
            obj.put("penaltyStatus", r.penaltyStatus.name)
            obj.put("excuseType", r.excuseType ?: "")
            obj.put("excuseNote", r.excuseNote ?: "")
            obj.put("approvedBy", r.approvedBy ?: "")
            obj.put("note", r.note ?: "")
            obj.put("manualEditReason", r.manualEditReason ?: "")
            recArray.put(obj)
        }
        root.put("attendance", recArray)

        val holidays = holidayDao.getAllHolidaysSync()
        val holArray = JSONArray()
        holidays.forEach { h ->
            val obj = JSONObject()
            obj.put("date", h.date)
            obj.put("name", h.name)
            holArray.put(obj)
        }
        root.put("holidays", holArray)

        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Result<Unit> {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("employees")) {
                val empArray = root.getJSONArray("employees")
                val empList = mutableListOf<Employee>()
                for (i in 0 until empArray.length()) {
                    val obj = empArray.getJSONObject(i)
                    empList.add(
                        Employee(
                            id = obj.optLong("id", 0L),
                            code = obj.getString("code"),
                            name = obj.getString("name"),
                            department = obj.getString("department"),
                            phone = obj.optString("phone", ""),
                            dailyWage = obj.optDouble("dailyWage", 0.0),
                            isActive = obj.optBoolean("isActive", true),
                            annualLeaveBalance = obj.optInt("annualLeaveBalance", 21),
                            sickLeaveBalance = obj.optInt("sickLeaveBalance", 14)
                        )
                    )
                }
                employeeDao.insertEmployees(empList)
            }

            if (root.has("attendance")) {
                val recArray = root.getJSONArray("attendance")
                val recList = mutableListOf<AttendanceRecord>()
                for (i in 0 until recArray.length()) {
                    val obj = recArray.getJSONObject(i)
                    recList.add(
                        AttendanceRecord(
                            employeeId = obj.getLong("employeeId"),
                            date = obj.getString("date"),
                            checkInTime = obj.optString("checkInTime").ifEmpty { null },
                            checkOutTime = obj.optString("checkOutTime").ifEmpty { null },
                            status = try {
                                AttendanceStatus.valueOf(obj.getString("status"))
                            } catch (e: Exception) {
                                AttendanceStatus.NOT_ARRIVED
                            },
                            latenessSeconds = obj.optInt("latenessSeconds", 0),
                            earlyDepartureSeconds = obj.optInt("earlyDepartureSeconds", 0),
                            overtimeSeconds = obj.optInt("overtimeSeconds", 0),
                            penaltyAmount = obj.optDouble("penaltyAmount", 0.0),
                            penaltyStatus = try {
                                PenaltyStatus.valueOf(obj.getString("penaltyStatus"))
                            } catch (e: Exception) {
                                PenaltyStatus.NONE
                            },
                            excuseType = obj.optString("excuseType").ifEmpty { null },
                            excuseNote = obj.optString("excuseNote").ifEmpty { null },
                            approvedBy = obj.optString("approvedBy").ifEmpty { null },
                            note = obj.optString("note").ifEmpty { null },
                            manualEditReason = obj.optString("manualEditReason").ifEmpty { null }
                        )
                    )
                }
                attendanceDao.insertRecords(recList)
            }

            auditLogDao.insertLog(
                AuditLog(
                    action = "Import JSON Backup",
                    employeeName = "System",
                    details = "Imported database backup from JSON file",
                    performedBy = "Manager"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
