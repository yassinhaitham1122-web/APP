package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Employee::class,
        AttendanceRecord::class,
        Holiday::class,
        LeaveRecord::class,
        AuditLog::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun holidayDao(): HolidayDao
    abstract fun leaveDao(): LeaveDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_tracker_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val settingsDao = database.settingsDao()
                settingsDao.saveSettings(AppSettings())

                val employeeDao = database.employeeDao()
                val initialEmployees = listOf(
                    Employee(
                        code = "EMP-101",
                        name = "أحمد محمد محمود (Ahmed Mahmoud)",
                        department = "تطوير البرمجيات (Engineering)",
                        phone = "01012345678",
                        dailyWage = 450.0,
                        isActive = true,
                        annualLeaveBalance = 21,
                        sickLeaveBalance = 14
                    ),
                    Employee(
                        code = "EMP-102",
                        name = "سارة إبراهيم حسن (Sara Ibrahim)",
                        department = "الموارد البشرية (HR)",
                        phone = "01123456789",
                        dailyWage = 400.0,
                        isActive = true,
                        annualLeaveBalance = 19,
                        sickLeaveBalance = 14
                    ),
                    Employee(
                        code = "EMP-103",
                        name = "محمود عادل الشريف (Mahmoud El-Sherif)",
                        department = "المحاسبة والمالية (Finance)",
                        phone = "01234567890",
                        dailyWage = 420.0,
                        isActive = true,
                        annualLeaveBalance = 18,
                        sickLeaveBalance = 12
                    ),
                    Employee(
                        code = "EMP-104",
                        name = "مريم خالد علي (Mariam Khaled)",
                        department = "التسويق الرقمي (Marketing)",
                        phone = "01545678901",
                        dailyWage = 380.0,
                        isActive = true,
                        annualLeaveBalance = 21,
                        sickLeaveBalance = 14
                    ),
                    Employee(
                        code = "EMP-105",
                        name = "عمر فاروق عبد الله (Omar Farouk)",
                        department = "خدمة العملاء (Support)",
                        phone = "01098765432",
                        dailyWage = 350.0,
                        isActive = true,
                        annualLeaveBalance = 15,
                        sickLeaveBalance = 10
                    ),
                    Employee(
                        code = "EMP-106",
                        name = "نور الدين مصطفى (Nour Mostafa)",
                        department = "المبيعات (Sales)",
                        phone = "01187654321",
                        dailyWage = 500.0,
                        isActive = true,
                        annualLeaveBalance = 20,
                        sickLeaveBalance = 13
                    )
                )
                employeeDao.insertEmployees(initialEmployees)

                val holidayDao = database.holidayDao()
                holidayDao.insertHolidays(
                    listOf(
                        Holiday(date = "2026-01-07", name = "عيد الميلاد المجيد (Coptic Christmas)"),
                        Holiday(date = "2026-01-25", name = "ثورة 25 يناير وعيد الشرطة (Revolution Day)"),
                        Holiday(date = "2026-04-25", name = "عيد تحرير سيناء (Sinai Liberation)"),
                        Holiday(date = "2026-05-01", name = "عيد العمال (Labour Day)"),
                        Holiday(date = "2026-07-23", name = "ثورة 23 يوليو (Revolution Day)"),
                        Holiday(date = "2026-10-06", name = "عيد القوات المسلحة (Armed Forces Day)")
                    )
                )

                val auditDao = database.auditLogDao()
                auditDao.insertLog(
                    AuditLog(
                        action = "System Initialization",
                        employeeName = "System",
                        details = "Initialized Attendance Tracker with default settings and staff directory",
                        performedBy = "System"
                    )
                )
            }
        }
    }
}
