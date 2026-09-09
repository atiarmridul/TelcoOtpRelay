package com.mridul.telcootprelay

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmsReceiverContractTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Suppress("DEPRECATION")
    @Test
    fun receiverRemainsExportedAndProtectedForSmsBroadcasts() {
        val component = ComponentName(context, SmsReceiver::class.java)
        val receiver = context.packageManager.getReceiverInfo(component, 0)
        val resolvedReceivers = context.packageManager.queryBroadcastReceivers(
            Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).setPackage(context.packageName),
            0
        )

        assertTrue(receiver.exported)
        assertEquals(Manifest.permission.BROADCAST_SMS, receiver.permission)
        assertTrue(resolvedReceivers.any { it.activityInfo.name == SmsReceiver::class.java.name })
    }

    @Suppress("DEPRECATION")
    @Test
    fun appRequestsOnlyItsExistingPermissions() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val userRelevantPermissions = packageInfo.requestedPermissions.orEmpty()
            .filterNot {
                it == "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            }
            .toSet()

        assertEquals(
            setOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.FOREGROUND_SERVICE
            ),
            userRelevantPermissions
        )
    }

    @Test
    fun receiverIgnoresNonSmsActions() {
        SmsReceiver().onReceive(context, Intent("com.mridul.telcootprelay.SYNTHETIC"))
    }
}
