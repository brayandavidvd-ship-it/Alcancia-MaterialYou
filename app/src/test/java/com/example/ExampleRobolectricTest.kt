package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ReminderHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Alcancía", appName)
  }

  @Test
  fun `reminder custom message updates and resets to default`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ReminderHelper.resetReminderMessage(context)
    assertEquals(
      ReminderHelper.DEFAULT_REMINDER_MESSAGE,
      ReminderHelper.getReminderMessage(context)
    )

    val custom = "Recuerda apartar tu ahorro semanal hoy."
    ReminderHelper.setReminderMessage(context, custom)
    assertEquals(custom, ReminderHelper.getReminderMessage(context))

    ReminderHelper.setReminderMessage(context, "   ")
    assertEquals(
      ReminderHelper.DEFAULT_REMINDER_MESSAGE,
      ReminderHelper.getReminderMessage(context)
    )

    ReminderHelper.setReminderMessage(context, custom)
    val restored = ReminderHelper.resetReminderMessage(context)
    assertEquals(ReminderHelper.DEFAULT_REMINDER_MESSAGE, restored)
    assertEquals(
      ReminderHelper.DEFAULT_REMINDER_MESSAGE,
      ReminderHelper.getReminderMessage(context)
    )
  }
}
