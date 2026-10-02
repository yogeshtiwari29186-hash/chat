package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calling.ActiveCallState
import com.example.calling.CallManager
import com.example.ui.components.UserAvatar
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary

@Composable
fun ActiveCallOverlay(
    callManager: CallManager
) {
    val callState by callManager.callState.collectAsState()

    if (callState.status == ActiveCallState.CallStatus.IDLE) return

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated Camera Video Preview if video call
            if (callState.isVideo && callState.isCameraOn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MeshTealPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Direct P2P Encrypted Video Feed",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Adaptive Bitrate: 450 kbps • Low Latency",
                            color = MeshTealPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Top Caller Info Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(name = callState.peerName, size = 80)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = callState.peerName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (callState.status) {
                        ActiveCallState.CallStatus.OUTGOING_CALLING -> "Ringing over mesh..."
                        ActiveCallState.CallStatus.INCOMING_RINGING -> "Incoming ${if (callState.isVideo) "Video" else "Voice"} Call"
                        ActiveCallState.CallStatus.CONNECTED -> formatCallDuration(callState.durationSeconds)
                        ActiveCallState.CallStatus.ENDED -> "Call Ended"
                        else -> ""
                    },
                    color = if (callState.status == ActiveCallState.CallStatus.CONNECTED) MeshTealPrimary else Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // Multi-hop warning if not direct radio link
                callState.warningMessage?.let { warn ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = warn,
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Call Controls
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    IconButton(
                        onClick = { callManager.toggleMute() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (callState.isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (callState.isMuted) Color.Black else Color.White
                        )
                    }

                    // Video Camera Toggle if video call
                    if (callState.isVideo) {
                        IconButton(
                            onClick = { callManager.toggleCamera() },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (!callState.isCameraOn) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (callState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Camera",
                                tint = if (!callState.isCameraOn) Color.Black else Color.White
                            )
                        }
                    }

                    // Speakerphone Button
                    IconButton(
                        onClick = { callManager.toggleSpeaker() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (callState.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            tint = if (callState.isSpeakerOn) Color.Black else Color.White
                        )
                    }

                    // End Call Button
                    IconButton(
                        onClick = { callManager.endCall() },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatCallDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
