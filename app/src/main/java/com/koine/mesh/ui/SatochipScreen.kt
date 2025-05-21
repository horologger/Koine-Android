package com.koine.mesh.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.koine.mesh.satochip.client.Constants
import com.koine.mesh.satochip.client.SatochipParser

@Composable
fun SatochipScreen(
    viewModel: SatochipViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var showChangePinDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = uiState) {
            is SatochipUiState.Initial -> {
                Text("Ready to connect to Satochip")
            }
            is SatochipUiState.Loading -> {
                CircularProgressIndicator()
            }
            is SatochipUiState.Success -> {
                CardStatus(state.status)
            }
            is SatochipUiState.SignatureSuccess -> {
                Text("Signature generated successfully")
            }
            is SatochipUiState.PublicKeySuccess -> {
                Text("Public key retrieved successfully")
            }
            is SatochipUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.verifyPin(pin) }
            ) {
                Text("Verify PIN")
            }
            
            Button(
                onClick = { showChangePinDialog = true }
            ) {
                Text("Change PIN")
            }
        }
        
        if (showChangePinDialog) {
            AlertDialog(
                onDismissRequest = { showChangePinDialog = false },
                title = { Text("Change PIN") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Current PIN") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("New PIN") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.changePin(pin, newPin)
                            showChangePinDialog = false
                        }
                    ) {
                        Text("Change")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showChangePinDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CardStatus(status: SatochipParser.ApplicationStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Card Status",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusItem("Initialized", status.isInitialized)
            StatusItem("Authenticated", status.isAuthenticated)
            Text("PIN Retry Count: ${status.pinRetryCount}")
            Text("PUK Retry Count: ${status.pukRetryCount}")
            StatusItem("Has Master Key", status.hasMasterKey)
            StatusItem("Has Auth Key", status.hasAuthenticationKey)
            StatusItem("Has Encryption Key", status.hasEncryptionKey)
            StatusItem("Has Signature Key", status.hasSignatureKey)
        }
    }
}

@Composable
private fun StatusItem(label: String, value: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(if (value) "Yes" else "No")
    }
} 