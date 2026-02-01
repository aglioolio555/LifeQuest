package com.example.lifequest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifequest.utils.AppUtils
import com.example.lifequest.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWhitelistScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val installedApps = remember { AppUtils.getInstalledApps(context) }
    val allowedApps by viewModel.allowedApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("許可アプリ設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(installedApps) { app ->
                val isChecked = allowedApps.any { it.packageName == app.packageName }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            if (checked) viewModel.addAllowedApp(app)
                            else viewModel.removeAllowedApp(app)
                        }
                    )
                }
            }
        }
    }
}