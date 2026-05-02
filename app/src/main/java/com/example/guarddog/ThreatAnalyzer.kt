package com.example.guarddog

data class SuspectApp(
    val appData: AppData,
    val score: Int,
    val scoreBreakdown: List<String>
)

object ThreatAnalyzer {

    fun analyze(appData: AppData): SuspectApp? {
        // Base Requirement: The app must exist in both the 7-day install list AND the 24-hour usage list.
        // For our SMS logic to work immediately, we skip this check ONLY if we explicitly tell it to via a flag,
        // but the core prompt said "Base Requirement for Evaluation: ...".
        // However, we decided via user request that SMS happens immediately.
        // We will make `enforceUsageRequirement` a parameter.
        return evaluateApp(appData)
    }

    fun evaluateApp(appData: AppData, enforceUsageRequirement: Boolean = true): SuspectApp? {
        if (enforceUsageRequirement) {
            val recentlyInstalled = System.currentTimeMillis() - appData.firstInstallTime <= 7L * 24 * 60 * 60 * 1000
            val recentlyUsed = appData.lastTimeUsed > 0L
            if (!recentlyInstalled || !recentlyUsed) {
                return null
            }
        }

        var score = 0
        val breakdown = mutableListOf<String>()

        // [ +1 Point ] Overlay Risk: Requests SYSTEM_ALERT_WINDOW
        if (appData.permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
            score += 1
            breakdown.add("Requests permission to show pop-ups over other apps (Overlay Risk).")
        }

        // [ +1 Point ] Sideloaded: Installer package is NOT the Google Play Store.
        if (appData.isSideloaded) {
            score += 1
            breakdown.add("App was not installed from the Google Play Store (Sideloaded).")
        }

        // [ +1 Point ] Home Hijacker: Registers as CATEGORY_HOME.
        if (appData.isHomeApp) {
            score += 1
            breakdown.add("Attempts to replace your Home screen (Home Hijacker).")
        }

        // [ +2 Points ] Hidden App: Does NOT register as CATEGORY_LAUNCHER
        if (!appData.isLauncherApp) {
            score += 2
            breakdown.add("Has no visible app icon (Hidden App).")
        }

        // [ +3 Points ] Financial/2FA Risk: Requests SEND_SMS, READ_SMS, or RECEIVE_SMS.
        val smsPermissions = listOf("android.permission.SEND_SMS", "android.permission.READ_SMS", "android.permission.RECEIVE_SMS")
        if (appData.permissions.any { it in smsPermissions }) {
            score += 3
            breakdown.add("Can read or send text messages (Financial/2FA Risk).")
        }

        // [ +3 Points ] Hostage Risk: Requests BIND_DEVICE_ADMIN or BIND_ACCESSIBILITY_SERVICE.
        val hostagePermissions = listOf("android.permission.BIND_DEVICE_ADMIN", "android.permission.BIND_ACCESSIBILITY_SERVICE")
        if (appData.permissions.any { it in hostagePermissions }) {
            score += 3
            breakdown.add("Requests deep system control permissions (Hostage Risk).")
        }

        return SuspectApp(
            appData = appData,
            score = score,
            scoreBreakdown = breakdown
        )
    }
}
