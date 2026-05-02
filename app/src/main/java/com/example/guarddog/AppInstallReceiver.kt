package com.example.guarddog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return

            val repository = AppRepository(context)
            val settingsManager = SettingsManager(context)

            val appData = repository.getAppInfo(packageName) ?: return

            // Bypassing 24-hour rule on install as per user request
            val suspectApp = ThreatAnalyzer.evaluateApp(appData, enforceUsageRequirement = false)

            if (suspectApp != null && suspectApp.score >= 3) {
                val phoneNumber = settingsManager.trustedContactPhone
                if (!phoneNumber.isNullOrEmpty()) {
                    sendSmsWarning(context, phoneNumber, suspectApp)
                }
            }
        }
    }

    private fun sendSmsWarning(context: Context, phoneNumber: String, suspectApp: SuspectApp) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val appName = getAppName(context, suspectApp.appData.packageName)
            val message = "GuardDog Alert: A high-risk app ($appName) was just installed on my phone."

            smsManager?.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            Log.e("GuardDog", "Failed to send SMS", e)
        }
    }

    private fun getAppName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
