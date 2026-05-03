package com.example.guarddog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ThreatAnalyzerTest {

    private fun createMockAppData(
        firstInstallTime: Long = System.currentTimeMillis() - 1000,
        lastTimeUsed: Long = System.currentTimeMillis() - 1000,
        permissions: List<String> = emptyList(),
        isHomeApp: Boolean = false,
        isLauncherApp: Boolean = true,
        isSideloaded: Boolean = false
    ): AppData {
        return AppData(
            packageName = "com.test.app",
            firstInstallTime = firstInstallTime,
            lastTimeUsed = lastTimeUsed,
            permissions = permissions,
            isHomeApp = isHomeApp,
            isLauncherApp = isLauncherApp,
            isSideloaded = isSideloaded
        )
    }

    @Test
    fun `safe app scores zero points`() {
        val appData = createMockAppData(
            isLauncherApp = true, // Has visible icon
            isSideloaded = false // Installed from Play Store
        )

        val result = ThreatAnalyzer.evaluateApp(appData)

        assertNotNull(result)
        assertEquals(0, result?.score)
        assertEquals(0, result?.scoreBreakdown?.size)
    }

    @Test
    fun `hidden app scores two points`() {
        val appData = createMockAppData(
            isLauncherApp = false // No visible icon
        )

        val result = ThreatAnalyzer.evaluateApp(appData)

        assertNotNull(result)
        assertEquals(2, result?.score)
        assertEquals(1, result?.scoreBreakdown?.size)
    }

    @Test
    fun `malicious app scores maximum points`() {
        val appData = createMockAppData(
            isLauncherApp = false, // Hidden (+2)
            isSideloaded = true, // Sideloaded (+1)
            isHomeApp = true, // Home Hijacker (+1)
            permissions = listOf(
                "android.permission.SYSTEM_ALERT_WINDOW", // Overlay (+1)
                "android.permission.SEND_SMS", // Financial (+3)
                "android.permission.BIND_DEVICE_ADMIN" // Hostage (+3)
            )
        )

        val result = ThreatAnalyzer.evaluateApp(appData)

        assertNotNull(result)
        // 2 + 1 + 1 + 1 + 3 + 3 = 11
        assertEquals(11, result?.score)
        assertEquals(6, result?.scoreBreakdown?.size)
    }

    @Test
    fun `enforceUsageRequirement blocks old and unused apps`() {
        val oldInstallTime = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000) // 8 days ago
        val appData = createMockAppData(
            firstInstallTime = oldInstallTime,
            lastTimeUsed = 0L // Never used
        )

        val result = ThreatAnalyzer.evaluateApp(appData, enforceUsageRequirement = true)

        assertNull(result)
    }

    @Test
    fun `enforceUsageRequirement allows recent apps`() {
        val recentInstallTime = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000) // 2 days ago
        val appData = createMockAppData(
            firstInstallTime = recentInstallTime,
            lastTimeUsed = System.currentTimeMillis() - 1000 // Used recently
        )

        val result = ThreatAnalyzer.evaluateApp(appData, enforceUsageRequirement = true)

        assertNotNull(result)
    }
}
