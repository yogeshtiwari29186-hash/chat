package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MeshPulseApplication
import com.example.ui.admin.AdminTab
import com.example.ui.admin.AdminViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.MeshTealDark
import com.example.ui.theme.MeshTealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(MeshPulseApplication.instance.repository))
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    var pinInput by remember { mutableStateOf("") }

    if (!uiState.isAuthenticated) {
        // Secure Admin PIN Authentication Prompt
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Owner & Admin Portal") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MeshTealPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Admin Authentication",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Enter the platform owner PIN to access advertising analytics and network moderation controls (Default: 1234)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                        )

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("Admin PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        uiState.authError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.verifyPin(pinInput) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
                        ) {
                            Text("Unlock Admin Dashboard")
                        }
                    }
                }
            }
        }
        return
    }

    // Authenticated Dashboard
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Owner & Admin Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowCreateAdDialog(true) }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "New Ad", tint = MeshTealPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dashboard Tabs (Advertisements, Users, Groups, Telemetry)
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == AdminTab.ADVERTISEMENTS,
                    onClick = { viewModel.selectTab(AdminTab.ADVERTISEMENTS) },
                    text = { Text("Ads & Stats") }
                )
                Tab(
                    selected = uiState.selectedTab == AdminTab.USERS,
                    onClick = { viewModel.selectTab(AdminTab.USERS) },
                    text = { Text("Users") }
                )
                Tab(
                    selected = uiState.selectedTab == AdminTab.GROUPS,
                    onClick = { viewModel.selectTab(AdminTab.GROUPS) },
                    text = { Text("Groups") }
                )
                Tab(
                    selected = uiState.selectedTab == AdminTab.TELEMETRY,
                    onClick = { viewModel.selectTab(AdminTab.TELEMETRY) },
                    text = { Text("Telemetry") }
                )
            }

            when (uiState.selectedTab) {
                AdminTab.ADVERTISEMENTS -> {
                    AdminAdsTab(
                        ads = uiState.ads,
                        onToggleActive = { viewModel.toggleAdActive(it) },
                        onDelete = { viewModel.deleteAd(it.adId) }
                    )
                }
                AdminTab.USERS -> {
                    AdminUsersTab(
                        contacts = uiState.contacts,
                        onToggleBlock = { viewModel.toggleBlockUser(it) }
                    )
                }
                AdminTab.GROUPS -> {
                    AdminGroupsTab(
                        groups = uiState.groups,
                        onToggleAds = { viewModel.toggleGroupAds(it) }
                    )
                }
                AdminTab.TELEMETRY -> {
                    AdminTelemetryTab()
                }
            }
        }
    }

    if (uiState.showCreateAdDialog) {
        CreateAdDialog(
            onDismiss = { viewModel.setShowCreateAdDialog(false) },
            onCreate = { title, desc, url, cta ->
                viewModel.createAdvertisement(title, desc, url, cta)
            }
        )
    }
}

@Composable
private fun AdminAdsTab(
    ads: List<com.example.data.local.entity.AdvertisementEntity>,
    onToggleActive: (com.example.data.local.entity.AdvertisementEntity) -> Unit,
    onDelete: (com.example.data.local.entity.AdvertisementEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val totalImpressions = ads.sumOf { it.impressions }
                    val totalClicks = ads.sumOf { it.clicks }
                    val avgCtr = if (totalImpressions > 0) (totalClicks.toDouble() / totalImpressions * 100) else 0.0

                    StatBox(label = "Active Campaigns", value = ads.count { it.isActive }.toString())
                    StatBox(label = "Total Impressions", value = totalImpressions.toString())
                    StatBox(label = "Total Clicks", value = totalClicks.toString())
                    StatBox(label = "Overall CTR", value = "%.1f%%".format(avgCtr))
                }
            }
        }

        items(ads) { ad ->
            val ctr = if (ad.impressions > 0) (ad.clicks.toDouble() / ad.impressions * 100) else 0.0

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ad.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Switch(
                            checked = ad.isActive,
                            onCheckedChange = { onToggleActive(ad) }
                        )
                    }

                    Text(ad.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Impressions: ${ad.impressions}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Clicks: ${ad.clicks}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("CTR: %.1f%%".format(ctr), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MeshTealPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onDelete(ad) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Ad")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MeshTealPrimary)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdminUsersTab(
    contacts: List<com.example.data.local.entity.ContactEntity>,
    onToggleBlock: (com.example.data.local.entity.ContactEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Registered Mesh Identities (${contacts.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(contacts) { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(name = contact.displayName, size = 42)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(contact.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("ID: ${contact.userId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (contact.isBlocked) {
                        Text("BLOCKED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                Button(
                    onClick = { onToggleBlock(contact) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (contact.isBlocked) MeshTealDark else MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (contact.isBlocked) "Unblock" else "Block", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminGroupsTab(
    groups: List<com.example.data.local.entity.GroupEntity>,
    onToggleAds: (com.example.data.local.entity.GroupEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Public & Private Mesh Groups (${groups.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(groups) { group ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (group.isPublic) MeshTealDark.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                if (group.isPublic) "PUBLIC" else "PRIVATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(group.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allows Group Ads:", fontSize = 12.sp)
                        Switch(
                            checked = group.allowsAds,
                            onCheckedChange = { onToggleAds(group) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTelemetryTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Decentralized Network Telemetry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                TelemetryRow(label = "Store-and-Forward Queue", value = "14 Encrypted Packets")
                TelemetryRow(label = "Mesh Cryptographic Suite", value = "ECDH NIST P-256 + AES-GCM-256")
                TelemetryRow(label = "Average Relay Hop Latency", value = "180 ms per hop")
                TelemetryRow(label = "Packet Loop Avoidance", value = "Seen-ID Bloom Filter + Visited Set")
                TelemetryRow(label = "Max Relay TTL", value = "6 Hops")
                TelemetryRow(label = "Packet Storage Expiry", value = "72 Hours")
                TelemetryRow(label = "Relay Plaintext Exposure", value = "0% (Zero-Knowledge)")
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MeshTealPrimary)
    }
}

@Composable
private fun CreateAdDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, url: String, cta: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://example.com/promo") }
    var cta by remember { mutableStateOf("Learn More") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Platform Advertisement") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Ad Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Destination URL / Action") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cta,
                    onValueChange = { cta = it },
                    label = { Text("Button CTA Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && desc.isNotBlank()) {
                        onCreate(title, desc, url, cta)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MeshTealDark)
            ) {
                Text("Publish Ad")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
