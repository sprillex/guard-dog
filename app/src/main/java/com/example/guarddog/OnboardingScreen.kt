package com.example.guarddog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(uiState: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current

    val usageStatsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { viewModel.checkPermissions() }
    )

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.checkPermissions() }
    )

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                        if (nameIndex != -1 && phoneIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            val phone = cursor.getString(phoneIndex)
                            viewModel.saveTrustedContact(name, phone)
                        }
                        cursor.close()
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!uiState.hasUsageStatsPermission) {
            Text("GuardDog needs Usage Access to detect recently used apps to protect you from malicious software.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                usageStatsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Text("Grant Usage Access")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (!uiState.hasSmsPermission) {
            Text("GuardDog can notify a trusted contact if a dangerous app is installed. We need SMS permission to do this.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS)
            }) {
                Text("Grant SMS Permission")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState.hasUsageStatsPermission && uiState.hasSmsPermission) {
            if (uiState.trustedContactName == null) {
                Text("Select a trusted contact to notify in case of a threat.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPickerLauncher.launch(intent)
                }) {
                    Text("Select Trusted Contact")
                }
            } else {
                Text("Trusted Contact: ${uiState.trustedContactName}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPickerLauncher.launch(intent)
                }) {
                    Text("Change Trusted Contact")
                }
            }
        }
    }
}
