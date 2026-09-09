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
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
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
        context.deleteDatabase("relay_history.db")
    }

    @After
    fun removeStoredState() {
        preferences().edit().clear().commit()
        context.deleteDatabase("relay_history.db")
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
    fun versionFooterStaysPinnedToTheBottomOfTheScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val version = activity.findViewById<View>(R.id.version_text)
                val screen = version.parent as View

                assertEquals(screen.height, version.bottom)
            }
        }
    }

    @Test
    fun persistedConfigurationRestoresExistingValuesAndActiveState() {
        preferences().edit()
            .putString(RelaySettings.WEBHOOK_KEY, "https://chat.googleapis.com/synthetic")
            .putString(RelaySettings.SENDERS_KEY, "BANK-OTP")
            .putString(RelaySettings.RECEIVING_NUMBER_KEY, "+8801812345678")
            .putString(RelaySettings.MODE_KEY, RelayMode.ALL_MESSAGES.name)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    "https://chat.googleapis.com/synthetic",
                    activity.findViewById<EditText>(R.id.webhook_edit).text.toString()
                )
                assertEquals(
                    "BANK-OTP",
                    activity.findViewById<EditText>(R.id.senders_edit).text.toString()
                )
                assertEquals(
                    "+8801812345678",
                    activity.findViewById<EditText>(R.id.receiving_number_edit).text.toString()
                )
                assertEquals(1, activity.findViewById<Spinner>(R.id.relay_mode_spinner).selectedItemPosition)
                assertEquals("Active", activity.findViewById<TextView>(R.id.status_text).text.toString())
                assertFalse(activity.findViewById<Button>(R.id.save_button).isEnabled)
            }
        }
    }

    @Test
    fun latestRelayShowsMetadataWithoutSmsBodyOrOtp() {
        preferences().edit()
            .putString(LastRelayStore.RESULT_KEY, RelayDeliveryResult.DELIVERED.name)
            .putString(LastRelayStore.SENDER_KEY, "TEST-SENDER")
            .putLong(LastRelayStore.TIMESTAMP_KEY, 0L)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.status_card).visibility)
                assertEquals(
                    "Last message delivered",
                    activity.findViewById<TextView>(R.id.last_relay_status).text.toString()
                )
                assertEquals(
                    "From: TEST-SENDER",
                    activity.findViewById<TextView>(R.id.last_relay_sender).text.toString()
                )
                val visibleMetadata = listOf(
                    activity.findViewById<TextView>(R.id.last_relay_status).text,
                    activity.findViewById<TextView>(R.id.last_relay_sender).text,
                    activity.findViewById<TextView>(R.id.last_relay_time).text
                ).joinToString(" ")
                assertFalse(visibleMetadata.contains("482193"))
                assertFalse(visibleMetadata.contains("Your code"))
            }
        }
    }

    @Test
    fun failedRelayRetainsExistingFailureDetail() {
        preferences().edit()
            .putString(LastRelayStore.RESULT_KEY, RelayDeliveryResult.FAILED.name)
            .putString(LastRelayStore.SENDER_KEY, "TEST-SENDER")
            .putLong(LastRelayStore.TIMESTAMP_KEY, 0L)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    "Last delivery failed",
                    activity.findViewById<TextView>(R.id.last_relay_status).text.toString()
                )
                assertEquals(
                    View.VISIBLE,
                    activity.findViewById<View>(R.id.last_relay_detail).visibility
                )
                assertEquals(
                    "Automatic retries exhausted.",
                    activity.findViewById<TextView>(R.id.last_relay_detail).text.toString()
                )
            }
        }
    }

    @Test
    fun latestRelayCardOpensLocalHistoryWithFailureReason() {
        preferences().edit()
            .putString(LastRelayStore.RESULT_KEY, RelayDeliveryResult.FAILED.name)
            .putString(LastRelayStore.SENDER_KEY, "TEST-SENDER")
            .putLong(LastRelayStore.TIMESTAMP_KEY, 0L)
            .putString(LastRelayStore.FAILURE_KIND_KEY, RelayFailureKind.DNS_FAILURE.name)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.status_card).performClick()
            }
            onView(withText("Relay history")).check(matches(isDisplayed()))
            onView(withText("Delivery failed")).check(matches(isDisplayed()))
            onView(withText("From: TEST-SENDER")).check(matches(isDisplayed()))
            onView(withText("Could not resolve the Google Chat host after retries."))
                .check(matches(isDisplayed()))
            onView(withText("Clear history")).check(matches(isDisplayed()))
            pressBack()
        }
    }

    @Test
    fun noInternetFailureAppearsInStatusAndHistory() {
        preferences().edit()
            .putString(LastRelayStore.RESULT_KEY, RelayDeliveryResult.FAILED.name)
            .putString(LastRelayStore.SENDER_KEY, "TEST-SENDER")
            .putLong(LastRelayStore.TIMESTAMP_KEY, 0L)
            .putString(LastRelayStore.FAILURE_KIND_KEY, RelayFailureKind.NO_INTERNET.name)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withText("No internet connection was available.")).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.status_card).performClick()
            }
            onView(withText("No internet connection was available.")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun relayHistoryStoresEveryResultNewestFirst() {
        RelayHistoryStore.append(
            context,
            LastRelayStatus(RelayDeliveryResult.DELIVERED, "FIRST-SENDER", 1L)
        )
        RelayHistoryStore.append(
            context,
            LastRelayStatus(
                RelayDeliveryResult.FAILED,
                "SECOND-SENDER",
                2L,
                RelayFailureKind.HTTP_STATUS,
                503
            )
        )

        val history = RelayHistoryStore.loadAll(context)

        assertEquals(2, history.size)
        assertEquals("SECOND-SENDER", history[0].status.sender)
        assertEquals(RelayFailureKind.HTTP_STATUS, history[0].status.failureKind)
        assertEquals(503, history[0].status.httpStatusCode)
        assertEquals("FIRST-SENDER", history[1].status.sender)
    }

    @Test
    fun clearHistoryRequiresConfirmationAndRemovesLocalResults() {
        val status = LastRelayStatus(RelayDeliveryResult.FAILED, "TEST-SENDER", 2L)
        RelayHistoryStore.append(context, status)
        LastRelayStore.save(preferences(), status.result, status.sender, status.timestampMillis)

        ActivityScenario.launch(RelayHistoryActivity::class.java).use { scenario ->
            onView(withText("Clear history")).perform(click())
            onView(withText("Clear relay history?")).check(matches(isDisplayed()))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withText("No relay history yet.")).check(matches(isDisplayed()))
            scenario.onActivity {
                assertTrue(RelayHistoryStore.loadAll(it).isEmpty())
                assertEquals(null, LastRelayStore.load(preferences()))
            }
        }
    }

    @Test
    fun webhookVisibilityControlKeepsExistingToggleBehavior() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webhook = activity.findViewById<EditText>(R.id.webhook_edit)
                val toggle = activity.findViewById<ImageButton>(R.id.webhook_visibility_button)

                toggle.performClick()
                assertEquals(null, webhook.transformationMethod)
                assertEquals("Hide webhook URL", toggle.contentDescription.toString())

                toggle.performClick()
                assertTrue(webhook.transformationMethod is PasswordTransformationMethod)
                assertEquals("Show webhook URL", toggle.contentDescription.toString())
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
            pressBack()
        }
    }

    private fun preferences() = context.getSharedPreferences(
        RelaySettings.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
}
