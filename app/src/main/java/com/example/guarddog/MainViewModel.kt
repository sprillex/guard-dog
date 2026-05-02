package com.example.guarddog

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val context: Context) : ViewModel() {
    private val appRepository = AppRepository(context)
    private val settingsManager = SettingsManager(context)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun checkPermissions() {
        val hasUsageStats = checkUsageStatsPermission(context)
        val hasSms = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = hasUsageStats,
            hasSmsPermission = hasSms
        )
        if (hasUsageStats && hasSms) {
            loadApps()
        }
    }

    fun loadApps() {
        val rawApps = appRepository.getGatheredAppData()
        val suspectApps = rawApps.mapNotNull { ThreatAnalyzer.analyze(it) }
            .filter { it.score >= 3 }
            .sortedByDescending { it.appData.lastTimeUsed }

        _uiState.value = _uiState.value.copy(suspectApps = suspectApps)
    }

    fun getTrustedContactName(): String? = settingsManager.trustedContactName
    fun getTrustedContactPhone(): String? = settingsManager.trustedContactPhone

    fun saveTrustedContact(name: String, phone: String) {
        settingsManager.trustedContactName = name
        settingsManager.trustedContactPhone = phone
        _uiState.value = _uiState.value.copy(
            trustedContactName = name,
            trustedContactPhone = phone
        )
    }

    companion object {
        fun checkUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}

data class UiState(
    val hasUsageStatsPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val suspectApps: List<SuspectApp> = emptyList(),
    val trustedContactName: String? = null,
    val trustedContactPhone: String? = null
)

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
