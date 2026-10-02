package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onComplete: (name: String, username: String, about: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("Using MeshPulse • Private & Offline") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(90.dp).clip(CircleShape).background(MeshTealPrimary.copy(alpha=.15f)), contentAlignment=Alignment.Center) {
            Icon(Icons.Default.Hub, null, tint=MeshTealPrimary, modifier=Modifier.size(52.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Create your permanent profile", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center)
        Text("Your Firebase identity and username are bound to this account.", textAlign=TextAlign.Center, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(vertical=12.dp))
        OutlinedTextField(name,{name=it},label={Text("Name")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(username,{username=it.lowercase().replace(" ","_")},label={Text("Username")},leadingIcon={Icon(Icons.Default.AlternateEmail,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(about,{about=it},label={Text("About")},singleLine=true,modifier=Modifier.fillMaxWidth())
        (errorMessage ?: error)?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(8.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(onClick={
            if(name.isBlank()) error="Name is required"
            else if(username.length<3) error="Username must be at least 3 characters"
            else onComplete(name.trim(),username.trim(),about.trim())
        },modifier=Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=MeshTealDark)) {
            Icon(Icons.Default.VpnKey,null); Spacer(Modifier.width(8.dp)); Text("Create permanent identity",fontWeight=FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Text("Your username can be used by other users to find your profile.",fontSize=11.sp,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
