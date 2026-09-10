package com.example.guarddog

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppData(
    val packageName: String,
    val firstInstallTime: Long,
    val lastTimeUsed: Long,
    val permissions: List<String>,
    val isHomeApp: Boolean,
    val isLauncherApp: Boolean,
    val isSideloaded: Boolean
)

/**
 * Repository class responsible for aggregating data from the Android OS.
 * Interfaces with PackageManager and UsageStatsManager to extract a unified
 * view of installed apps, their usage history, and their system capabilities.
 */
class AppRepository(private val context: Context) {

    /**
     * Gathers data for all non-system apps installed within the last 7 days.
     * Executes heavy queries on a background IO thread to prevent UI blocking.
     *
     * @return A list of [AppData] objects containing raw package and usage information.
     */
    suspend fun getGatheredAppData(): List<AppData> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 1. Fetch Recent Installs (last 7 days)
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.timeInMillis

        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        val recentInstalls = packages.filter {
            val isSystemApp = (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            !isSystemApp && it.firstInstallTime >= sevenDaysAgo && it.packageName != context.packageName
        }

        // 2. Fetch Recent Usage (last 24 hours)
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val twentyFourHoursAgo = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            twentyFourHoursAgo,
            now
        )
        val usedApps = usageStats.filter { it.lastTimeUsed > 0 }.associateBy { it.packageName }

        // 3. Identify Launchers & Home Apps
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homeApps = packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .toSet()

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        // 4. Combine and check Installer Source
        recentInstalls.map { pkg ->
            val pkgName = pkg.packageName
            val usage = usedApps[pkgName]

            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    packageManager.getInstallSourceInfo(pkgName).installingPackageName
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(pkgName)
            }

            val isSideloaded = installer != "com.android.vending"

            AppData(
                packageName = pkgName,
                firstInstallTime = pkg.firstInstallTime,
                lastTimeUsed = usage?.lastTimeUsed ?: 0L,
                permissions = pkg.requestedPermissions?.toList() ?: emptyList(),
                isHomeApp = homeApps.contains(pkgName),
                isLauncherApp = launcherApps.contains(pkgName),
                isSideloaded = isSideloaded
            )
        }
    }

    /**
     * Fetches detailed application data for a single specific package name.
     * Used primarily by the BroadcastReceiver when a new app is installed.
     *
     * @param packageName The specific package to query.
     * @return An [AppData] object, or null if the package is a system app or not found.
     */
    fun getAppInfo(packageName: String): AppData? {
        val packageManager = context.packageManager
        return try {
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

            val isSystemApp = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp) return null

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val twentyFourHoursAgo = calendar.timeInMillis

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                twentyFourHoursAgo,
                now
            )
            val usage = usageStats.find { it.packageName == packageName }

            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homeApps = packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
                .toSet()

            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()

            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    packageManager.getInstallSourceInfo(packageName).installingPackageName
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            val isSideloaded = installer != "com.android.vending"

            AppData(
                packageName = packageName,
                firstInstallTime = pkg.firstInstallTime,
                lastTimeUsed = usage?.lastTimeUsed ?: 0L,
                permissions = pkg.requestedPermissions?.toList() ?: emptyList(),
                isHomeApp = homeApps.contains(packageName),
                isLauncherApp = launcherApps.contains(packageName),
                isSideloaded = isSideloaded
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
