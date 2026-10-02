package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Physical Testing Guide", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "How to test MeshPulse on physical Android devices across Phases 1 through 7:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                PhaseCard(
                    phaseNumber = "1",
                    title = "Two-Device Encrypted Messaging",
                    steps = "1. Install APK on Phone A and Phone B.\n2. Turn off Wi-Fi and Mobile Data on both phones, but keep Bluetooth ON.\n3. Open Mesh tab on Phone A; Phone B's permanent User ID will be discovered.\n4. Send a message from Phone A to Phone B. Observe delivery state transition: WAITING -> RELAYING -> DELIVERED (✓✓).\n5. Decrypt message on Phone B; verify Delivery ACK arrives back at Phone A."
                )

                PhaseCard(
                    phaseNumber = "2",
                    title = "Multi-Hop Relay Between 3+ Devices",
                    steps = "1. Place Phone A (Sender) and Phone C (Recipient) out of radio range (e.g., 50 meters apart or separate rooms).\n2. Place Phone B (Relay node) in the middle.\n3. Send message from Phone A to Phone C. Phone B receives the packet, validates the routing envelope, sees it is not the recipient, and immediately relays it to Phone C.\n4. Verify in Phone B's database/log that the payload is strictly encrypted and Phone B CANNOT read the text."
                )

                PhaseCard(
                    phaseNumber = "3",
                    title = "Store-and-Forward Mobility",
                    steps = "1. Turn off Bluetooth on Phone C or walk Phone C away entirely.\n2. Phone A sends message to Phone C. Phone B stores it in its pending relay table (RelayPacketEntity).\n3. Move Phone B near Phone C (or turn Phone C's radio back on).\n4. Phone B discovers Phone C, flushes the stored packet, and Phone C decrypts the message."
                )

                PhaseCard(
                    phaseNumber = "4",
                    title = "Delivery Status & Synchronization",
                    steps = "1. Watch the message status ticks on Phone A.\n2. In transit: ⏳ Relaying.\n3. Phone C receives and verifies AES-GCM tag: updates to ✓✓ (Delivered).\n4. Phone C opens the chat: sends READ_ACK back through Phone B -> turns blue ✓✓ on Phone A.\n5. Tap 'Message Info' to see sent, delivered, and read timestamps and hop count."
                )

                PhaseCard(
                    phaseNumber = "5",
                    title = "Direct Voice Calling",
                    steps = "1. On Phone A, tap the Voice Call icon for nearby Phone B.\n2. Phone B rings with the incoming mesh call alert.\n3. Tap Accept. Verify audio streaming, mute/unmute, and speakerphone toggles."
                )

                PhaseCard(
                    phaseNumber = "6",
                    title = "Direct Video Calling",
                    steps = "1. Launch video call between two direct peers.\n2. Note the P2P low-latency camera stream.\n3. If routed over multi-hop, verify the prominent bandwidth warning indicator appears."
                )

                PhaseCard(
                    phaseNumber = "7",
                    title = "Background Operation & Hardening",
                    steps = "1. Minimize the app or lock the screen.\n2. Observe the persistent MeshPulse notification indicating background relay is active.\n3. Send a message to the device; verify message arrives even with app minimized."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun PhaseCard(phaseNumber: String, title: String, steps: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MeshTealDark
                ) {
                    Text(
                        text = "Phase $phaseNumber",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = steps,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
