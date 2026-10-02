package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DeliveryStatusIcon(
    status: String,
    modifier: Modifier = Modifier
) {
    when (status.uppercase()) {
        "WAITING" -> {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = "Waiting in queue",
                tint = TickGray,
                modifier = modifier.size(14.dp)
            )
        }
        "RELAYING" -> {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = "Relaying through mesh",
                tint = StatusRelayingAmber,
                modifier = modifier.size(14.dp)
            )
        }
        "SENT" -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = TickGray,
                modifier = modifier.size(15.dp)
            )
        }
        "DELIVERED" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered to recipient",
                tint = TickGray,
                modifier = modifier.size(16.dp)
            )
        }
        "READ" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read by recipient",
                tint = TickBlue,
                modifier = modifier.size(16.dp)
            )
        }
        "FAILED" -> {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Delivery failed",
                tint = StatusFailedRed,
                modifier = modifier.size(14.dp)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = TickGray,
                modifier = modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    avatarUri: String? = null,
    modifier: Modifier = Modifier,
    size: Int = 44,
    hasStatusRing: Boolean = false,
    isOnline: Boolean = false
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    val avatarBg = rememberAvatarColor(name)

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hasStatusRing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MeshTealPrimary)
            )
        }

        Box(
            modifier = Modifier
                .size(if (hasStatusRing) (size - 4).dp else size.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.42).sp
            )
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size * 0.3).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MeshPulseAccent)
            )
        }
    }
}

@Composable
fun EncryptionBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "E2E Encrypted",
                tint = EncryptionGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Messages are end-to-end encrypted. Intermediate relay phones and networks cannot read your messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun SignalStrengthBadge(rssi: Int, isDirect: Boolean, hopCount: Int = 1) {
    Surface(
        color = if (isDirect) MeshTealDark.copy(alpha = 0.2f) else StatusRelayingAmber.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDirect) Icons.Default.BluetoothConnected else Icons.Default.Share,
                contentDescription = null,
                tint = if (isDirect) MeshTealPrimary else StatusRelayingAmber,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isDirect) "Direct ($rssi dBm)" else "$hopCount Hops Relay",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDirect) MeshTealPrimary else StatusRelayingAmber
            )
        }
    }
}

fun rememberAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF00796B),
        Color(0xFF1E88E5),
        Color(0xFF5E35B1),
        Color(0xFFD81B60),
        Color(0xFF00897B),
        Color(0xFFFB8C00),
        Color(0xFF43A047),
        Color(0xFF8E24AA)
    )
    val hash = kotlin.math.abs(name.hashCode())
    return colors[hash % colors.size]
}
