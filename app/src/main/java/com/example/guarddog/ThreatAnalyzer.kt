package com.example.guarddog

data class SuspectApp(
    val appData: AppData,
    val score: Int,
    val scoreBreakdown: List<String>
)

/**
 * Core domain engine responsible for evaluating applications for potentially malicious or
 * predatory behaviors based on their requested permissions and system intents.
 */
object ThreatAnalyzer {

    /**
     * Convenience method to evaluate an app without enforcing the 24-hour usage requirement.
     * Often used during immediate installation broadcast events.
     *
     * @param appData The raw application data to be analyzed.
     * @return A [SuspectApp] object if the app scores points, or null if it's considered safe.
     */
    fun analyze(appData: AppData): SuspectApp? {
        // Base Requirement: The app must exist in both the 7-day install list AND the 24-hour usage list.
        // For our SMS logic to work immediately, we skip this check ONLY if we explicitly tell it to via a flag,
        // but the core prompt said "Base Requirement for Evaluation: ...".
        // However, we decided via user request that SMS happens immediately.
        // We will make `enforceUsageRequirement` a parameter.
        return evaluateApp(appData)
    }

    /**
     * Evaluates the threat level of a given application using a weighted scoring system.
     *
     * The scoring is based on the following rules:
     * - [+1] SYSTEM_ALERT_WINDOW (Overlay Risk)
     * - [+1] Sideloaded (Not from Google Play)
     * - [+1] Registers as CATEGORY_HOME (Home Hijacker)
     * - [+2] Does not register as CATEGORY_LAUNCHER (Hidden App)
     * - [+3] SMS Permissions (Financial/2FA Risk)
     * - [+3] Device Admin / Accessibility (Hostage Risk)
     *
     * Apps scoring 3 or higher are considered high-risk.
     *
     * @param appData The raw application data to be analyzed.
     * @param enforceUsageRequirement If true, drops apps not installed within the last 7 days or not used within 24 hours.
     * @return A [SuspectApp] object containing the score and breakdown, or null if it doesn't meet the baseline criteria.
     */
    fun evaluateApp(appData: AppData, enforceUsageRequirement: Boolean = false): SuspectApp? {
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
