package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.ui.chat.ChatViewModel
import com.example.ui.components.DeliveryStatusIcon
import com.example.ui.components.EncryptionBanner
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onStartVoiceCall: (peerId: String, peerName: String) -> Unit,
    onStartVideoCall: (peerId: String, peerName: String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val conversationTitle = uiState.conversation?.title ?: "Chat"
    val isGroup = uiState.conversation?.isGroup == true
    val recipientId = uiState.conversation?.recipientUserId ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (isGroup) viewModel.toggleGroupInfo(true)
                        }
                    ) {
                        UserAvatar(name = conversationTitle, size = 38)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = conversationTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isGroup) {
                                    "${uiState.groupMembers.size} members • Mesh Broadcast"
                                } else if (uiState.isRecipientNearby) {
                                    "Direct Radio • 1 Hop"
                                } else {
                                    "Store-and-Forward Mesh (Relay)"
                                },
                                fontSize = 11.sp,
                                color = if (uiState.isRecipientNearby) MeshTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isGroup) {
                        IconButton(onClick = { onStartVoiceCall(recipientId, conversationTitle) }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MeshTealPrimary)
                        }
                        IconButton(onClick = { onStartVideoCall(recipientId, conversationTitle) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MeshTealPrimary)
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isGroup) {
                            DropdownMenuItem(
                                text = { Text("Group Info & Ads") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleGroupInfo(true)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Clear Messages") },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                messages.forEach { viewModel.deleteMessage(it) }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Reply Preview
                uiState.replyingToMessage?.let { replyMsg ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = MeshTealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (replyMsg.isIncoming) conversationTitle else "You",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MeshTealPrimary
                                )
                                Text(
                                    text = replyMsg.content,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAttachmentSheet = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Encrypted mesh message...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MeshTealPrimary
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                coroutineScope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MeshTealPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            EncryptionBanner()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages, key = { it.messageId }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onLongClick = { viewModel.selectMessageForInfo(message) },
                        onReply = { viewModel.setReplyingTo(message) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    // Message Info Bottom Sheet
    uiState.selectedMessageForInfo?.let { msg ->
        MessageInfoBottomSheet(
            message = msg,
            receipts = uiState.messageReceipts,
            onDismiss = { viewModel.selectMessageForInfo(null) },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Mesh Message", msg.content))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                viewModel.selectMessageForInfo(null)
            },
            onDelete = {
                viewModel.deleteMessage(msg)
                viewModel.selectMessageForInfo(null)
            },
            onReply = {
                viewModel.setReplyingTo(msg)
                viewModel.selectMessageForInfo(null)
            }
        )
    }

    // Attachment Picker Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Send Encrypted Media", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        color = Color(0xFFE91E63),
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendMessage("📷 Captured photo", mediaType = "IMAGE")
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Image,
                        label = "Gallery",
                        color = Color(0xFF9C27B0),
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendMessage("🖼️ Mesh photo asset", mediaType = "IMAGE")
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Mic,
                        label = "Voice Note",
                        color = Color(0xFFFF9800),
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendMessage("🎤 Voice note (0:08)", mediaType = "VOICE_NOTE")
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.InsertDriveFile,
                        label = "Document",
                        color = Color(0xFF2196F3),
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendMessage("📄 offline_mesh_protocol.pdf", mediaType = "DOCUMENT")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Group Info Sheet
    if (uiState.showGroupInfo) {
        GroupInfoDialog(
            group = uiState.group,
            members = uiState.groupMembers,
            onDismiss = { viewModel.toggleGroupInfo(false) },
            onToggleAds = { viewModel.toggleGroupAds(it) },
            onAddMember = { id, name -> viewModel.addGroupMember(id, name) },
            onRemoveMember = { id -> viewModel.removeGroupMember(id) },
            onPromoteAdmin = { id -> viewModel.promoteGroupAdmin(id) },
            onOnlyAdminsEdit = { viewModel.setOnlyAdminsCanEditInfo(it) },
            onOnlyAdminsSend = { viewModel.setOnlyAdminsCanSendMessages(it) },
            onApprovalRequired = { viewModel.setApprovalRequired(it) },
            onInviteLinkEnabled = { viewModel.setInviteLinkEnabled(it) },
            onDisappearing = { viewModel.setDisappearingMessages(it) }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: MessageEntity,
    onLongClick: () -> Unit,
    onReply: () -> Unit
) {
    val isOutgoing = !message.isIncoming

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isOutgoing) MeshDarkBubbleOutgoing else MeshDarkBubbleIncoming,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isOutgoing) 14.dp else 2.dp,
                bottomEnd = if (isOutgoing) 2.dp else 14.dp
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable { onLongClick() }
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                // Reply quote if applicable
                if (!message.replyToText.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(MeshTealPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.replyToText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Media Banner if any
                if (message.mediaType != "NONE") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        val icon = when (message.mediaType) {
                            "IMAGE" -> Icons.Default.Image
                            "VOICE_NOTE" -> Icons.Default.PlayCircleFilled
                            else -> Icons.Default.InsertDriveFile
                        }
                        Icon(icon, contentDescription = null, tint = MeshTealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[Encrypted ${message.mediaType}]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeshTealPrimary
                        )
                    }
                }

                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        fontSize = 10.sp,
                        color = TickGray
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        DeliveryStatusIcon(status = message.deliveryStatus)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInfoBottomSheet(
    message: MessageEntity,
    receipts: List<com.example.data.local.entity.MessageReceiptEntity>,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Message Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = message.content, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "ID: ${message.messageId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            InfoRow(
                icon = Icons.Default.Send,
                title = "Sent",
                desc = formatFullDate(message.timestamp),
                status = "Dispatched into mesh outbox"
            )

            InfoRow(
                icon = Icons.Default.HourglassTop,
                title = "Mesh Relay Routing",
                desc = if (message.hopCount > 0) "${message.hopCount} Hops Traversed" else "Direct Radio Link",
                status = "Relay phones carry encrypted payload"
            )

            InfoRow(
                icon = Icons.Default.DoneAll,
                title = "Delivered",
                desc = message.deliveredTimestamp?.let { formatFullDate(it) } ?: "Waiting for delivery ACK",
                status = if (message.deliveredTimestamp != null) "✓✓ Recipient phone acknowledged" else "⏳ Relaying"
            )

            InfoRow(
                icon = Icons.Default.DoneAll,
                title = "Read",
                desc = message.readTimestamp?.let { formatFullDate(it) } ?: "Not read yet",
                status = if (message.readTimestamp != null) "✓✓ Blue Read ACK received" else "Waiting for recipient to open",
                isBlue = message.readTimestamp != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onReply) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reply")
                }
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, status: String, isBlue: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isBlue) TickBlue else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(status, fontSize = 11.sp, color = if (isBlue) TickBlue else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun GroupInfoDialog(
    group: com.example.data.local.entity.GroupEntity?,
    members: List<com.example.data.local.entity.GroupMemberEntity>,
    onDismiss: () -> Unit,
    onToggleAds: (Boolean) -> Unit,
    onAddMember: (String, String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteAdmin: (String) -> Unit,
    onOnlyAdminsEdit: (Boolean) -> Unit,
    onOnlyAdminsSend: (Boolean) -> Unit,
    onApprovalRequired: (Boolean) -> Unit,
    onInviteLinkEnabled: (Boolean) -> Unit,
    onDisappearing: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var showQr by remember { mutableStateOf(false) }
    val token = group?.inviteLinkToken ?: group?.inviteCode.orEmpty()
    val invite = "https://whatsapp-347ca.web.app/group/" + (group?.groupId.orEmpty()) + "?code=" + token
    val qrBitmap = remember(invite) { generateQrBitmap(invite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group?.name ?: "Group Info") },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp)) {
                Text(group?.description.orEmpty(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Invite link", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(invite, fontSize = 10.sp, color = MeshTealPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Group invite", invite)); Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show() }) { Text("Copy link") }
                    TextButton(onClick = { showQr = !showQr }) { Text(if (showQr) "Hide QR" else "Invite QR") }
                    TextButton(onClick = { showAdd = true }) { Text("Add") }
                }
                if (showQr) qrBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "Group invite QR", modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally)) }
                GroupSettingSwitch("Only admins edit group info", group?.onlyAdminsCanEditInfo == true, onOnlyAdminsEdit)
                GroupSettingSwitch("Only admins send messages", group?.onlyAdminsCanSendMessages == true, onOnlyAdminsSend)
                GroupSettingSwitch("Approve new members", group?.approvalRequired == true, onApprovalRequired)
                GroupSettingSwitch("Invite link enabled", group?.inviteLinkEnabled == true, onInviteLinkEnabled)
                GroupSettingSwitch("Group advertisements", group?.allowsAds == true, onToggleAds)
                Text("Disappearing messages", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0L to "Off", 86400L to "24h", 604800L to "7d").forEach { (seconds, label) ->
                        FilterChip(selected = group?.disappearingMessagesSeconds == seconds, onClick = { onDisappearing(seconds) }, label = { Text(label) })
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("MEMBERS (${members.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                members.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(name = m.displayName, size = 30)
                        Spacer(Modifier.width(8.dp))
                        Text(m.displayName, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(m.role, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        if (m.role == "MEMBER") {
                            TextButton(onClick = { onPromoteAdmin(m.userId) }) { Text("Admin", fontSize = 10.sp) }
                            IconButton(onClick = { onRemoveMember(m.userId) }) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showAdd) {
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Add group member") },
            text = { Column {
                OutlinedTextField(userId, { userId = it }, label = { Text("User ID / username") }, singleLine = true)
                OutlinedTextField(userName, { userName = it }, label = { Text("Display name") }, singleLine = true)
            } },
            confirmButton = { TextButton(onClick = { onAddMember(userId, userName); userId = ""; userName = ""; showAdd = false }, enabled = userId.isNotBlank() && userName.isNotBlank()) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } })
    }
}

@Composable
private fun GroupSettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 12.sp); Switch(checked = checked, onCheckedChange = onCheckedChange) }
}

private fun generateQrBitmap(value: String): Bitmap? = try {
    val matrix: BitMatrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 420, 420)
    Bitmap.createBitmap(420, 420, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (x in 0 until 420) for (y in 0 until 420) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
} catch (_: Exception) { null }

fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
