package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MeshPulseApplication
import com.example.data.local.entity.AdvertisementEntity
import com.example.data.local.entity.StatusStoryEntity
import com.example.ui.components.UserAvatar
import com.example.ui.status.StatusViewModel
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary
import kotlinx.coroutines.delay

@Composable
fun StatusScreenInline(
    onOpenStatusView: () -> Unit,
    viewModel: StatusViewModel = viewModel(factory = StatusViewModel.Factory(MeshPulseApplication.instance.repository))
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // My Status Item
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (uiState.myStories.isNotEmpty()) {
                            viewModel.openStory(uiState.myStories.first())
                        } else {
                            viewModel.setShowCreateDialog(true)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    UserAvatar(name = "You", size = 52, hasStatusRing = uiState.myStories.isNotEmpty())
                    if (uiState.myStories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MeshTealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Status", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        text = if (uiState.myStories.isNotEmpty()) {
                            "Tap to view • ${uiState.myStories.first().viewsCount} views"
                        } else {
                            "Tap to add status update"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.setShowCreateDialog(true) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Create Status", tint = MeshTealPrimary)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }

        // Recent Peer Updates Section
        if (uiState.activeStories.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT UPDATES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            items(uiState.activeStories) { story ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openStory(story) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(name = story.userName, size = 52, hasStatusRing = true)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(story.userName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            text = formatMessageTime(story.createdAt),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Sponsored Announcements / Advertisement Section
        if (uiState.sponsoredAds.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPONSORED ANNOUNCEMENTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Admin Verified",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            items(uiState.sponsoredAds) { ad ->
                LaunchedEffect(ad.adId) {
                    viewModel.onAdImpression(ad)
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = MeshTealPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ad.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = ad.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.onAdClicked(ad) },
                            colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(ad.actionText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Create Status Dialog
    if (uiState.showCreateDialog) {
        CreateStatusDialog(
            onDismiss = { viewModel.setShowCreateDialog(false) },
            onCreate = { text, color, privacy ->
                viewModel.createStatus(text, null, color, privacy)
            }
        )
    }

    // Story Fullscreen Viewer Dialog
    uiState.viewingStory?.let { story ->
        StoryViewerDialog(
            story = story,
            viewers = uiState.storyViewers,
            onClose = { viewModel.closeStory() }
        )
    }

    // Ad Click Detail Dialog
    uiState.showAdDetailsDialog?.let { ad ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdDialog() },
            title = { Text(ad.title) },
            text = {
                Column {
                    Text(ad.description, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Action Destination: ${ad.actionUrl ?: "https://example.com/mesh"}", fontSize = 12.sp, color = MeshTealPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Interaction recorded in Decentralized Analytics.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAdDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onCreate: (text: String, backgroundColor: Long, privacy: String) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF005C4BL) }
    var selectedPrivacy by remember { mutableStateOf("CONTACTS") }

    val colorOptions = listOf(
        0xFF005C4BL, // Emerald Dark
        0xFF008069L, // Teal
        0xFF1E88E5L, // Blue
        0xFF5E35B1L, // Deep Purple
        0xFFD81B60L  // Crimson
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Status Update") },
        text = {
            Column {
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    placeholder = { Text("What's on your mind? (24h ephemeral)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Background Color:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorVal) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Privacy: Contacts only • E2E Encrypted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (statusText.isNotBlank()) {
                        onCreate(statusText.trim(), selectedColor, selectedPrivacy)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
            ) {
                Text("Post Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StoryViewerDialog(
    story: StatusStoryEntity,
    viewers: List<com.example.data.local.entity.StatusViewEntity>,
    onClose: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var showViewersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(story.statusId) {
        val totalMs = 5000
        val interval = 50
        var elapsed = 0
        while (elapsed < totalMs) {
            delay(interval.toLong())
            elapsed += interval
            progress = elapsed.toFloat() / totalMs
        }
        onClose()
    }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxSize(),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        text = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(story.backgroundColor))
                    .padding(20.dp)
            ) {
                // Top progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                // User Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(name = story.userName, size = 36)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(story.userName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text(formatMessageTime(story.createdAt), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Story Content text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.text,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Bottom Viewers Row
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable { showViewersSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${story.viewsCount} views", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {}
    )

    if (showViewersSheet) {
        AlertDialog(
            onDismissRequest = { showViewersSheet = false },
            title = { Text("Viewed by (${viewers.size})") },
            text = {
                Column {
                    if (viewers.isEmpty()) {
                        Text("No one has viewed this status yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        viewers.forEach { v ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = v.viewerName, size = 32)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(v.viewerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(formatMessageTime(v.viewedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewersSheet = false }) { Text("Close") }
            }
        )
    }
}
