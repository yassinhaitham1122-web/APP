package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val department: String,
    val phone: String = "",
    val dailyWage: Double = 0.0,
    val isActive: Boolean = true,
    val annualLeaveBalance: Int = 21,
    val sickLeaveBalance: Int = 14,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus {
    NOT_ARRIVED,
    ON_TIME,
    LATE,
    ABSENT,
    EXCUSED,
    DAY_OFF
}

enum class PenaltyStatus {
    NONE,
    PENDING,
    APPLIED,
    EXCUSED,
    WAIVED
}

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val date: String, // Format: YYYY-MM-DD
    val checkInTime: String? = null, // Format: HH:mm:ss
    val checkOutTime: String? = null, // Format: HH:mm:ss
    val status: AttendanceStatus = AttendanceStatus.NOT_ARRIVED,
    val latenessSeconds: Int = 0,
    val earlyDepartureSeconds: Int = 0,
    val overtimeSeconds: Int = 0,
    val penaltyAmount: Double = 0.0,
    val penaltyStatus: PenaltyStatus = PenaltyStatus.NONE,
    val excuseType: String? = null, // Sick, Emergency, Work Assignment, Prior Permission, Other
    val excuseNote: String? = null,
    val approvedBy: String? = null,
    val note: String? = null,
    val manualEditReason: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val name: String
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val employeeName: String = "",
    val details: String,
    val performedBy: String = "Manager"
)

@Entity(tableName = "leave_records")
data class LeaveRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val type: String, // Annual, Sick, Unpaid
    val startDate: String,
    val endDate: String,
    val daysCount: Int,
    val reason: String = "",
    val status: String = "Approved",
    val approvedAt: Long = System.currentTimeMillis()
)

enum class PenaltyRuleType {
    PER_MINUTE,
    TIERED,
    FIXED
}

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val workStartTime: String = "09:00:00",
    val workEndTime: String = "17:00:00",
    val gracePeriodMinutes: Int = 10,
    val weeklyDaysOff: String = "5,6", // 5 = Friday, 6 = Saturday (Calendar.FRIDAY=6, SATURDAY=7 or ISO 5,6)
    val penaltyRuleType: PenaltyRuleType = PenaltyRuleType.TIERED,
    val perMinuteRate: Double = 1.0,
    val tieredUpTo15Min: Double = 10.0,
    val tiered15To60Min: Double = 30.0,
    val tieredOver60MinDeductionFraction: Double = 0.5, // 0.5 = half day daily wage
    val fixedPenaltyAmount: Double = 50.0,
    val currency: String = "EGP",
    val timezone: String = "Africa/Cairo",
    val useArabicIndicDigits: Boolean = false,
    val isArabic: Boolean = true,
    val isDarkMode: Boolean = false,
    val managerPin: String = "1234",
    val pinProtectionEnabled: Boolean = false
)
