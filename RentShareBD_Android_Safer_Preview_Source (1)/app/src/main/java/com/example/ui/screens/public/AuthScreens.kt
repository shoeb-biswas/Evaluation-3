package com.example.ui.screens.public

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.domain.UserRole
import com.example.ui.components.RentShareTopBar
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    initialMode: String,
    onBack: (() -> Unit)?,
    onLogin: (email: String, password: String) -> Unit,
    onDemoLogin: (UserRole) -> Unit,
    onRegister: (fullName: String, email: String, phone: String, password: String, role: String) -> Unit,
    onResetPassword: (email: String, newPassword: String) -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) } // login, register, reset
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+88017") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("tenant") }

    Scaffold(
        topBar = {
            RentShareTopBar(
                title = when (mode) {
                    "register" -> "Create Account"
                    "reset" -> "Reset Password"
                    else -> "Sign In to RentShare BD"
                },
                onBack = onBack
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Preview only: accounts are stored on this device, not shared online. Do not enter sensitive information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("login" to "Sign In", "register" to "Register").forEachIndexed { i, (key, label) ->
                    SegmentedButton(
                        selected = mode == key,
                        onClick = { mode = key },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 3)
                    ) {
                        Text(label)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode == "register") {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("auth_fullname")
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_email")
                    )
                    if (mode == "register") {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("BD Phone (+880...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_phone")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = role == "tenant",
                                onClick = { role = "tenant" },
                                label = { Text("I'm a Tenant") }
                            )
                            FilterChip(
                                selected = role == "landlord",
                                onClick = { role = "landlord" },
                                label = { Text("I'm a Landlord") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (mode == "reset") "New Password (6+ chars)" else "Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password")
                    )
                    Button(
                        onClick = {
                            when (mode) {
                                "register" -> onRegister(fullName, email, phone, password, role)
                                "reset" -> onResetPassword(email, password)
                                else -> onLogin(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_submit")
                    ) {
                        Text(
                            when (mode) {
                                "register" -> "Create RentShare Account"
                                "reset" -> "Update Password"
                                else -> "Sign In"
                            }
                        )
                    }
                }
            }

            // 1-Tap Demo Credentials Card (debug builds only)
            if (BuildConfig.DEBUG) Card(
                colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = IndigoPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("1-Tap Demo Credentials (Pass: Demo1234!)", style = MaterialTheme.typography.titleSmall)
                    }
                    UserRole.entries.forEach { demoRole ->
                        OutlinedButton(
                            onClick = { onDemoLogin(demoRole) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_demo_${demoRole.key}")
                        ) {
                            Text("Continue as Demo ${demoRole.label}")
                        }
                    }
                }
            }
        }
    }
}
