package com.example.guarddog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                        ThreatListScreen(uiState)
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
}
