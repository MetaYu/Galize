package com.galize.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galize.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Configuration
            Text(text = "AI Configuration", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = apiBaseUrl,
                onValueChange = { viewModel.updateApiBaseUrl(it) },
                label = { Text("API Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            // Persona Selection
            Text(text = "Persona", style = MaterialTheme.typography.titleMedium)

            val personas = listOf(
                "default" to "Default (Balanced)",
                "cool_genius" to "Cool Genius (高冷学霸)",
                "hot_blooded" to "Hot Blooded (热血少年)",
                "sharp_tongue" to "Sharp Tongue (毒舌执事)"
            )

            personas.forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedPersona == key,
                        onClick = { viewModel.updatePersona(key) }
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
