# guard-dogApp Summary

A targeted, sideloaded Android utility designed to protect vulnerable users by identifying recently installed apps that exhibit malicious or predatory behaviors. It cross-references installation timestamps (last 7 days) with usage stats (last 24 hours) and applies a Threat Score based on requested permissions and system intents. High-risk apps are surfaced in a simplified UI with a direct, one-click uninstall prompt.  It also has the abiility to notify a user contact to warn of new app installations.

## Build & Installation

To build the project and create an APK for sideloading, run the following command from the repository root:

```bash
./gradlew assembleDebug
```

Once the APK is built (located in `app/build/outputs/apk/debug/app-debug.apk`), you can install it on a connected device using Android Debug Bridge (ADB):

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

This application follows the **MVVM (Model-View-ViewModel)** architectural pattern:
- **Model (Repositories):** `AppRepository` fetches raw package and usage stats from the Android system. `SettingsManager` manages encrypted local preferences. `ThreatAnalyzer` acts as the domain engine, converting raw app data into actionable `SuspectApp` objects.
- **ViewModel:** `MainViewModel` manages the background data-loading coroutines and exposes the current `UiState` via a `StateFlow`.
- **View (Jetpack Compose):** UI components like `ThreatListScreen` and `OnboardingScreen` observe the `UiState` and reactively update to display threats or request necessary permissions.

Phase 1: Data Gathering (The Repositories)

    Request Required Permissions: Ensure the app's AndroidManifest.xml includes PACKAGE_USAGE_STATS (requires settings redirect) and QUERY_ALL_PACKAGES (safe since you are sideloading).

    Fetch Recent Installs: * Use PackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS).

        Filter out system apps ((flags and ApplicationInfo.FLAG_SYSTEM) == 0).

        Filter for firstInstallTime within the last 7 days.

    Fetch Recent Usage: * Use UsageStatsManager.queryUsageStats().

        Set the query interval to the last 24 hours.

        Filter out apps with a lastTimeUsed of 0.

    Identify Launchers & Hidden Apps: * Query Intent.CATEGORY_HOME to build a list of apps trying to be the home screen.

        Query Intent.CATEGORY_LAUNCHER to build a list of apps that have a visible icon in the app drawer.

    Check Installer Source:

        Use PackageManager.getInstallSourceInfo() (API 30+) to verify if the app was installed by "com.android.vending" (Google Play).

Phase 2: The Threat Engine (Evaluation Logic)

Create a data class (e.g., SuspectApp) and map the merged data through this scoring system.

Base Requirement for Evaluation: The app must exist in both the 7-day install list AND the 24-hour usage list.

Threat Scoring (Flag if Score is ≥ 3):

    [ +1 Point ] Overlay Risk: Requests SYSTEM_ALERT_WINDOW (Pop-ups).

    [ +1 Point ] Sideloaded: Installer package is NOT the Google Play Store.

    [ +1 Point ] Home Hijacker: Registers as CATEGORY_HOME.

    [ +2 Points ] Hidden App: Does NOT register as CATEGORY_LAUNCHER (No app drawer icon).

    [ +3 Points ] Financial/2FA Risk: Requests SEND_SMS, READ_SMS, or RECEIVE_SMS.

    [ +3 Points ] Hostage Risk: Requests BIND_DEVICE_ADMIN or BIND_ACCESSIBILITY_SERVICE.

Phase 3: Presentation & Action (UI/UX)

    Build the List: Use Jetpack Compose to create a LazyColumn of the flagged SuspectApp objects, sorted by lastTimeUsed descending.

    Design for Accessibility:

        Use high-contrast text and large touch targets.

        Display the app icon prominently so she can visually recognize what she clicked.

        Translate the Threat Score into a plain-English warning (e.g., "Warning: This hidden app can read your text messages.").

    The Uninstall Action: * Provide a massive "Uninstall" button on each row.

        Wire the button to fire Intent.ACTION_DELETE with the package:[packageName] URI to trigger the native Android uninstall modal immediately.

Phase 4: Permissions Onboarding (First Run)

    Usage Stats Check: On app launch, check AppOpsManager for OPSTR_GET_USAGE_STATS.

    Settings Redirection: If missing, explain why the app needs it in plain text, then launch Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).
