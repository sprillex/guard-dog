package com.example.guarddog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this, MainViewModelFactory(applicationContext))[MainViewModel::class.java]
        viewModel.checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.hasUsageStatsPermission && uiState.hasSmsPermission && uiState.trustedContactPhone != null) {
                        LaunchedEffect(Unit) {
                            startGuardDogService()
                        }
                        ThreatListScreen(
                            uiState = uiState,
                            onRefresh = { viewModel.loadApps() }
                        )
                    } else {
                        OnboardingScreen(uiState, viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val viewModel = ViewModelProvider(this, MainViewModelFactory(applicationContext))[MainViewModel::class.java]
        viewModel.checkPermissions()
    }

    private fun startGuardDogService() {
        val serviceIntent = Intent(this, GuardDogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
