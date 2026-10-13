package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.i18n.formatDigits
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Attendance Tracker", appName)
    }

    @Test
    fun `test lateness calculation logic`() {
        // Start: 09:00:00, Grace: 10 min -> Threshold: 09:10:00 (33000 seconds)
        // CheckIn: 09:05:00 -> within grace period -> 0 lateness
        val onTimeLateness = TimeUtils.calculateLatenessSeconds(
            actualCheckIn = "09:05:00",
            officialStart = "09:00:00",
            graceMinutes = 10
        )
        assertEquals(0, onTimeLateness)

        // CheckIn: 09:25:00 -> 15 minutes late after grace period -> 900 seconds
        val lateLateness = TimeUtils.calculateLatenessSeconds(
            actualCheckIn = "09:25:00",
            officialStart = "09:00:00",
            graceMinutes = 10
        )
        assertEquals(15 * 60, lateLateness)
    }

    @Test
    fun `test bilingual translations and digit formatting`() {
        val enTitle = Translations.get("app_title", AppLanguage.EN)
        val arTitle = Translations.get("app_title", AppLanguage.AR)
        assertEquals("Attendance Tracker", enTitle)
        assertEquals("سجل الحضور والانصراف", arTitle)

        val westernStr = "12345"
        val arabicIndicStr = formatDigits(westernStr, useArabicIndic = true)
        assertEquals("١٢٣٤٥", arabicIndicStr)
    }
}
