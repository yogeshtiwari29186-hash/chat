package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.local.entity.UserAccountEntity
import com.example.ui.components.UserAvatar
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    user: UserAccountEntity?,
    onDismiss: () -> Unit,
    onUpdateProfile: (name: String, about: String, avatarUri: String?) -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var name by remember(user) { mutableStateOf(user?.displayName ?: "") }
    var about by remember(user) { mutableStateOf(user?.about ?: "") }
    var showSeedPhrase by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Permanent Mesh Identity", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar & Name Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = name.ifBlank { "You" }, size = 64)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MeshTealDark.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Permanent ID: ${user?.userId ?: "Generating..."}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeshTealPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Copy User ID Button
            OutlinedButton(
                onClick = {
                    user?.userId?.let {
                        clipboard.setPrimaryClip(ClipData.newPlainText("User ID", it))
                        Toast.makeText(context, "User ID copied: $it", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy My User ID to Share")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Edit Profile Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About Status") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onUpdateProfile(name.trim(), about.trim(), user?.avatarUri)
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Profile")
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Cryptographic Fingerprint
            Text("Cryptographic Security", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val fingerprint = user?.publicKey?.let { CryptoEngine.getPublicKeyFingerprint(it) } ?: "Unavailable"
            Text(
                text = "Key Fingerprint: $fingerprint\nAlgorithm: NIST P-256 (secp256r1) with AES-256-GCM\nDecentralization: Independent of SIM, Carrier, or Server",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seed Phrase Backup
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("12-Word Recovery Backup", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        TextButton(onClick = { showSeedPhrase = !showSeedPhrase }) {
                            Text(if (showSeedPhrase) "Hide" else "Reveal")
                        }
                    }

                    if (showSeedPhrase) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = user?.recoverySeed ?: "pulse cipher beacon relay orbit matrix haven solace crystal nexus zenith echo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MeshTealPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Write down this recovery seed in a safe place. If you change or reinstall your phone, this seed restores your identity.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
