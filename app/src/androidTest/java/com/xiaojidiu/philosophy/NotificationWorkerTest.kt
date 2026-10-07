package com.xiaojidiu.philosophy

import android.app.NotificationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.testing.TestListenableWorkerBuilder
import com.xiaojidiu.philosophy.data.local.PreferencesRepository
import com.xiaojidiu.philosophy.util.ReminderWorker
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.Assert.*
import java.io.FileInputStream

class NotificationWorkerTest {
    @Test fun reminderSendsOnlyWhenEnabled() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        if(Build.VERSION.SDK_INT>=33) InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").use {FileInputStream(it.fileDescriptor).readBytes()}
        val preferences=PreferencesRepository(context);val manager=context.getSystemService(NotificationManager::class.java)
        try {
            preferences.setReminder(true,20,0)
            TestListenableWorkerBuilder<ReminderWorker>(context).build().doWork()
            withTimeout(5000) {while(manager.activeNotifications.none {it.id==71}) delay(50)}
            assertTrue(manager.activeNotifications.any {it.id==71})
            manager.cancel(71);preferences.setReminder(false,20,0)
            withTimeout(5000) {while(manager.activeNotifications.any {it.id==71}) delay(50)}
            TestListenableWorkerBuilder<ReminderWorker>(context).build().doWork()
            assertTrue(manager.activeNotifications.none {it.id==71})
        } finally {manager.cancel(71);preferences.setReminder(false,20,0)}
    }
}
