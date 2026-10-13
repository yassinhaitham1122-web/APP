package com.example.data

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): Employee?

    @Query("SELECT * FROM employees WHERE code = :code LIMIT 1")
    suspend fun getEmployeeByCode(code: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeeById(id: Long)

    @Query("UPDATE employees SET annualLeaveBalance = :annual, sickLeaveBalance = :sick WHERE id = :id")
    suspend fun updateLeaveBalances(id: Long, annual: Int, sick: Int)

    @Query("SELECT * FROM employees")
    suspend fun getAllEmployeesSync(): List<Employee>
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY id ASC")
    fun getRecordsByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, id DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND date = :date LIMIT 1")
    suspend fun getRecord(employeeId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getRecordsForEmployee(employeeId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date < :currentDate AND checkInTime IS NOT NULL AND (checkOutTime IS NULL OR checkOutTime = '')")
    suspend fun getMissingCheckOutRecords(currentDate: String): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>)

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllRecordsSync(): List<AttendanceRecord>
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    suspend fun getHolidayByDate(date: String): Holiday?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<Holiday>)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHolidayById(id: Long)

    @Query("SELECT * FROM holidays")
    suspend fun getAllHolidaysSync(): List<Holiday>
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_records ORDER BY approvedAt DESC")
    fun getAllLeaves(): Flow<List<LeaveRecord>>

    @Query("SELECT * FROM leave_records WHERE employeeId = :employeeId ORDER BY startDate DESC")
    fun getLeavesForEmployee(employeeId: Long): Flow<List<LeaveRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveRecord): Long

    @Query("DELETE FROM leave_records WHERE id = :id")
    suspend fun deleteLeaveById(id: Long)

    @Query("SELECT * FROM leave_records")
    suspend fun getAllLeavesSync(): List<LeaveRecord>
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()

    @Query("SELECT * FROM audit_logs")
    suspend fun getAllLogsSync(): List<AuditLog>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}
