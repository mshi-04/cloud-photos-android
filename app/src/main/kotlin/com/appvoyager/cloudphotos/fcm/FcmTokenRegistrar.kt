package com.appvoyager.cloudphotos.fcm

import android.content.Context
import com.appvoyager.cloudphotos.data.fcm.DeviceToken
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FcmTokenRegistrar @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun register() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val rawToken = task.result
            if (rawToken.isNullOrBlank()) {
                return@addOnCompleteListener
            }
            val deviceToken = try {
                DeviceToken.of(rawToken)
            } catch (_: IllegalArgumentException) {
                return@addOnCompleteListener
            }
            RegisterDeviceTokenWorker.enqueue(context, deviceToken)
        }
    }
}
