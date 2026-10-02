package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary

@Composable
fun OnboardingScreen(
    errorMessage: String? = null,
    onCreate: (name: String, username: String, email: String, password: String, about: String) -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onResetPassword: (email: String) -> Unit
) {
    var loginMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("Using MeshPulse • Private & Offline") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(82.dp).clip(CircleShape).background(MeshTealPrimary.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Hub, null, tint = MeshTealPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (loginMode) "Login to your account" else "Create your permanent account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Firebase Email + Password",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Mail, null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        if (!loginMode) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                username, { username = it.lowercase().replace(" ", "_") },
                label = { Text("Username") }, leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(about, { about = it }, label = { Text("About") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }

        (errorMessage ?: error)?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                error = when {
                    email.isBlank() -> "Email is required"
                    password.length < 6 -> "Password must be at least 6 characters"
                    !loginMode && name.isBlank() -> "Name is required"
                    !loginMode && username.length < 3 -> "Username must be at least 3 characters"
                    else -> null
                }
                if (error == null) {
                    if (loginMode) onLogin(email.trim(), password)
                    else onCreate(name.trim(), username.trim(), email.trim(), password, about.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
        ) {
            Icon(if (loginMode) Icons.Default.Lock else Icons.Default.VpnKey, null)
            Spacer(Modifier.width(8.dp))
            Text(if (loginMode) "Login" else "Create account", fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = {
            if (email.isNotBlank()) onResetPassword(email.trim())
            else error = "Enter your email first"
        }) { Text("Forgot password?") }

        TextButton(onClick = {
            loginMode = !loginMode
            error = null
        }) {
            Text(if (loginMode) "Create a new account" else "Already have an account? Login")
        }

        Text(
            "Your Firebase UID is the permanent account identity. Use the same email and password after reinstalling.",
            fontSize = 11.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
