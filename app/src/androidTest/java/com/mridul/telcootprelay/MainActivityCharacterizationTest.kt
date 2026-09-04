package com.mridul.telcootprelay

import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityCharacterizationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearStoredState() {
        preferences().edit().clear().commit()
    }

    @After
    fun removeStoredState() {
        preferences().edit().clear().commit()
    }

    @Test
    fun firstLaunchShowsSetupWithMaskedWebhookAndNoRelayHistory() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Setup", activity.findViewById<TextView>(R.id.status_text).text.toString())
                assertEquals(View.GONE, activity.findViewById<View>(R.id.status_card).visibility)
                assertEquals("Saved", activity.findViewById<Button>(R.id.save_button).text.toString())
                assertFalse(activity.findViewById<Button>(R.id.save_button).isEnabled)
                assertTrue(
                    activity.findViewById<EditText>(R.id.webhook_edit).transformationMethod is
                        PasswordTransformationMethod
                )
                assertEquals(
                    "Show webhook URL",
                    activity.findViewById<ImageButton>(R.id.webhook_visibility_button)
                        .contentDescription.toString()
                )
            }
        }
    }

    @Test
    fun selectingAllMessagesShowsItsExistingDescriptionAndPrivacyWarning() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val spinner = activity.findViewById<Spinner>(R.id.relay_mode_spinner)
                spinner.setSelection(1)
                spinner.onItemSelectedListener?.onItemSelected(
                    spinner,
                    spinner.selectedView,
                    1,
                    spinner.getItemIdAtPosition(1)
                )

                assertEquals(
                    "Relays every non-empty SMS, including personal messages.",
                    activity.findViewById<TextView>(R.id.mode_description).text.toString()
                )
                assertEquals(
                    View.VISIBLE,
                    activity.findViewById<View>(R.id.all_messages_privacy_warning).visibility
                )
                assertEquals("Unsaved", activity.findViewById<TextView>(R.id.status_text).text.toString())
                assertTrue(activity.findViewById<Button>(R.id.save_button).isEnabled)
            }
        }
    }

    @Test
    fun actionButtonsRetainEqualFortyEightDpControlsAndBackgrounds() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val testButton = activity.findViewById<Button>(R.id.test_button)
                val saveButton = activity.findViewById<Button>(R.id.save_button)
                val expectedHeight = (48 * activity.resources.displayMetrics.density).toInt()

                assertEquals(expectedHeight, testButton.layoutParams.height)
                assertEquals(expectedHeight, saveButton.layoutParams.height)
                assertEquals(testButton.layoutParams.width, saveButton.layoutParams.width)
                assertNotNull(testButton.background)
                assertNotNull(saveButton.background)
            }
        }
    }

    @Test
    fun privacyCardOpensTheExistingAboutAndPrivacyScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.privacy_card).performClick()
            }
            onView(withText("About & Privacy")).check(matches(isDisplayed()))
            onView(withText("Stored on this device")).check(matches(isDisplayed()))
        }
    }

    private fun preferences() = context.getSharedPreferences(
        RelaySettings.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
}
