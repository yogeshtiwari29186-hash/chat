package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calling.ActiveCallState
import com.example.data.local.entity.ConversationEntity
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.components.DeliveryStatusIcon
import com.example.ui.components.UserAvatar
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenConversation: (String) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenTestingGuide: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStatusView: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val nearbyPeers by viewModel.nearbyPeers.collectAsState()
    val callState by viewModel.callManager.callState.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MeshPulse",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MeshTealPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "OFFLINE MESH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MeshTealPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "● ${nearbyPeers.size} Peers in radio range",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                        UserAvatar(
                            name = uiState.currentUser?.displayName ?: "You",
                            size = 36,
                            hasStatusRing = true,
                            isOnline = true,
                            modifier = Modifier.clickable { onOpenProfile() }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setSearchActive(!uiState.isSearchActive) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { onOpenTestingGuide() }) {
                        Icon(Icons.Default.Science, contentDescription = "Testing Guide", tint = MeshTealPrimary)
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Group") },
                            leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.setShowNewGroupDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Admin & Ad Dashboard") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenAdminDashboard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Account & Security") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Physical Testing Guide") },
                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenTestingGuide()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            when (uiState.selectedTab) {
                MainTab.CHATS -> {
                    FloatingActionButton(
                        onClick = { viewModel.setShowNewChatDialog(true) },
                        containerColor = MeshTealPrimary,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "New Chat")
                    }
                }
                MainTab.STATUS -> {
                    FloatingActionButton(
                        onClick = { onOpenStatusView() },
                        containerColor = MeshTealPrimary,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Status")
                    }
                }
                MainTab.CALLS -> {
                    FloatingActionButton(
                        onClick = { viewModel.setShowNewChatDialog(true) },
                        containerColor = MeshTealPrimary,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "New Call")
                    }
                }
                MainTab.MESH_PEERS -> {
                    FloatingActionButton(
                        onClick = { viewModel.setShowNewChatDialog(true) },
                        containerColor = MeshTealPrimary,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Peer")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Incoming Call Alert Banner if ringing
            if (callState.status == ActiveCallState.CallStatus.INCOMING_RINGING) {
                Surface(
                    color = MeshTealDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(name = callState.peerName, size = 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = callState.peerName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (callState.isVideo) "Incoming Video Call • Mesh P2P" else "Incoming Voice Call • Mesh P2P",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { viewModel.callManager.declineIncomingCall() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.callManager.acceptIncomingCall() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MeshTealPrimary)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.Black)
                            }
                        }
                    }
                }
            }

            // Search bar if active
            if (uiState.isSearchActive) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search chats, contacts, or messages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.setSearchActive(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Tabs Row (Chats, Status, Calls, Mesh)
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MeshTealPrimary
            ) {
                Tab(
                    selected = uiState.selectedTab == MainTab.CHATS,
                    onClick = { viewModel.selectTab(MainTab.CHATS) },
                    text = {
                        val totalUnread = conversations.sumOf { it.unreadCount }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chats", fontWeight = FontWeight.SemiBold)
                            if (totalUnread > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = MeshTealPrimary) {
                                    Text(totalUnread.toString(), color = Color.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == MainTab.STATUS,
                    onClick = { viewModel.selectTab(MainTab.STATUS) },
                    text = { Text("Status", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = uiState.selectedTab == MainTab.CALLS,
                    onClick = { viewModel.selectTab(MainTab.CALLS) },
                    text = { Text("Calls", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = uiState.selectedTab == MainTab.MESH_PEERS,
                    onClick = { viewModel.selectTab(MainTab.MESH_PEERS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mesh", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(nearbyPeers.size.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                )
            }

            // Active Tab Content
            when (uiState.selectedTab) {
                MainTab.CHATS -> {
                    ChatsTabContent(
                        conversations = conversations.filter {
                            it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                            it.lastMessageText.contains(uiState.searchQuery, ignoreCase = true)
                        },
                        onOpenConversation = onOpenConversation
                    )
                }
                MainTab.STATUS -> {
                    StatusTabContent(onOpenStatusView = onOpenStatusView)
                }
                MainTab.CALLS -> {
                    CallsTabContent(
                        viewModel = viewModel,
                        callState = callState
                    )
                }
                MainTab.MESH_PEERS -> {
                    MeshPeersTabContent(
                        peers = nearbyPeers,
                        onPeerClick = { peer ->
                            viewModel.createNewDirectChat(peer.userId ?: peer.deviceId, peer.displayName) { convId ->
                                onOpenConversation(convId)
                            }
                        }
                    )
                }
            }
        }
    }

    // New Chat / Peer Dialog
    if (uiState.showNewChatDialog) {
        NewChatDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowNewChatDialog(false) },
            onChatStarted = { convId ->
                viewModel.setShowNewChatDialog(false)
                onOpenConversation(convId)
            }
        )
    }

    // New Group Dialog
    if (uiState.showNewGroupDialog) {
        NewGroupDialog(
            onDismiss = { viewModel.setShowNewGroupDialog(false) },
            onCreate = { name, desc ->
                viewModel.setShowNewGroupDialog(false)
                viewModel.createGroup(name, desc) { convId ->
                    onOpenConversation(convId)
                }
            }
        )
    }
}

@Composable
private fun ChatsTabContent(
    conversations: List<ConversationEntity>,
    onOpenConversation: (String) -> Unit
) {
    if (conversations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No conversations yet",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Tap the chat button or switch to the Mesh tab to message nearby peers.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(conversations, key = { it.conversationId }) { conv ->
                ConversationItem(conversation = conv, onClick = { onOpenConversation(conv.conversationId) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(name = conversation.title, size = 52)
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = formatMessageTime(conversation.lastMessageTimestamp),
                    fontSize = 12.sp,
                    color = if (conversation.unreadCount > 0) MeshTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!conversation.isGroup) {
                    DeliveryStatusIcon(status = conversation.lastMessageStatus)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = conversation.lastMessageText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MeshTealPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTabContent(onOpenStatusView: () -> Unit) {
    // Rendered via StatusScreen or embed directly
    StatusScreenInline(onOpenStatusView = onOpenStatusView)
}

@Composable
fun CallsTabContent(
    viewModel: MainViewModel,
    callState: ActiveCallState
) {
    val callHistory by viewModel.callHistory.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MeshTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Encrypted direct radio calls supported within Bluetooth/Wi-Fi range. Multi-hop relay calls may experience packet latency.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(callHistory) { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(name = record.peerName, size = 48)
                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.peerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = if (record.isIncoming) {
                            if (record.status == "MISSED") Icons.Default.CallMissed else Icons.Default.CallReceived
                        } else {
                            Icons.Default.CallMade
                        }
                        val tint = if (record.status == "MISSED") MaterialTheme.colorScheme.error else MeshTealPrimary

                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatMessageTime(record.timestamp)} • ${if (record.durationSeconds > 0) "${record.durationSeconds}s" else record.status}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { viewModel.startVoiceCall(record.peerId, record.peerName) }) {
                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MeshTealPrimary)
                }
                IconButton(onClick = { viewModel.startVideoCall(record.peerId, record.peerName) }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MeshTealPrimary)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun MeshPeersTabContent(
    peers: List<com.example.mesh.model.MeshPeer>,
    onPeerClick: (com.example.mesh.model.MeshPeer) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MeshTealPrimary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MeshTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mesh Network Engine Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MeshTealPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Store-and-forward relay node active. Packets from out-of-range users are safely carried and forwarded automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "DISCOVERED PEERS (${peers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(peers) { peer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPeerClick(peer) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(name = peer.displayName, size = 48, isOnline = true)
                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = peer.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "ID: ${peer.userId ?: peer.deviceId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (peer.isDirect) MeshTealDark.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = if (peer.isDirect) "1 Hop Direct" else "${peer.hopDistance} Hops Relay",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (peer.isDirect) MeshTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${peer.rssi} dBm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { onPeerClick(peer) },
                    colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Chat", fontSize = 12.sp)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun NewChatDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onChatStarted: (String) -> Unit
) {
    var peerIdInput by remember { mutableStateOf("") }
    var peerNameInput by remember { mutableStateOf("") }
    val contacts by viewModel.contacts.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Encrypted Chat") },
        text = {
            Column {
                Text(
                    text = "Select a known contact or enter a recipient's Permanent User ID:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = peerNameInput,
                    onValueChange = { peerNameInput = it },
                    label = { Text("Contact Name") },
                    placeholder = { Text("e.g. John") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = peerIdInput,
                    onValueChange = { peerIdInput = it.uppercase() },
                    label = { Text("User ID") },
                    placeholder = { Text("e.g. MP-8F72-29AC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (contacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Known Contacts:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    contacts.take(3).forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    peerNameInput = c.displayName
                                    peerIdInput = c.userId
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(name = c.displayName, size = 28)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(c.displayName, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (peerIdInput.isNotBlank()) {
                        val name = peerNameInput.ifBlank { "Peer ${peerIdInput.takeLast(4)}" }
                        viewModel.createNewDirectChat(peerIdInput.trim(), name, onChatStarted)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
            ) {
                Text("Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NewGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("Offline mesh discussion group") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Mesh Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Hiking Squad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = groupDesc,
                    onValueChange = { groupDesc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreate(groupName.trim(), groupDesc.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
