package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calling.CallManager
import com.example.ui.MainViewModel
import com.example.ui.chat.ChatViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MeshTealPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var callManager: CallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MeshPulseApplication
        callManager = CallManager(this, app.repository)

        setContent {
            MyApplicationTheme {
                val appRepository = app.repository
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(appRepository, callManager)
                )

                val uiState by mainViewModel.uiState.collectAsState()

                // Request Permissions
                RequestMeshPermissions()

                var activeConversationId by remember { mutableStateOf<String?>(null) }
                var showAdmin by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MeshTealPrimary)
                            }
                        }

                        !uiState.isAccountSetup -> {
                            OnboardingScreen(
                                errorMessage = uiState.accountError,
                                onCreate = { name, username, email, password, about ->
                                    mainViewModel.completeAccountCreation(name, username, email, password, about)
                                },
                                onLogin = { email, password ->
                                    mainViewModel.login(email, password)
                                },
                                onResetPassword = { email ->
                                    mainViewModel.resetPassword(email)
                                }
                            )
                        }

                        showAdmin -> {
                            AdminDashboardScreen(
                                onBack = { showAdmin = false }
                            )
                        }

                        activeConversationId != null -> {
                            val chatViewModel: ChatViewModel = viewModel(
                                key = activeConversationId,
                                factory = ChatViewModel.Factory(activeConversationId!!, appRepository)
                            )
                            ChatScreen(
                                viewModel = chatViewModel,
                                onBack = { activeConversationId = null },
                                onStartVoiceCall = { peerId, peerName ->
                                    mainViewModel.startVoiceCall(peerId, peerName)
                                },
                                onStartVideoCall = { peerId, peerName ->
                                    mainViewModel.startVideoCall(peerId, peerName)
                                }
                            )
                        }

                        else -> {
                            HomeScreen(
                                viewModel = mainViewModel,
                                onOpenConversation = { convId -> activeConversationId = convId },
                                onOpenAdminDashboard = { showAdmin = true },
                                onOpenTestingGuide = { mainViewModel.setShowTestingGuide(true) },
                                onOpenProfile = { mainViewModel.setShowProfileSheet(true) },
                                onOpenStatusView = { mainViewModel.selectTab(com.example.ui.MainTab.STATUS) }
                            )
                        }
                    }

                    // Active Call Overlay (Voice/Video)
                    ActiveCallOverlay(callManager = callManager)

                    // Profile & Security Sheet
                    if (uiState.showProfileSheet) {
                        ProfileBottomSheet(
                            user = uiState.currentUser,
                            onDismiss = { mainViewModel.setShowProfileSheet(false) },
                            onUpdateProfile = { name, about, avatarUri ->
                                mainViewModel.updateProfile(name, about, avatarUri)
                            }
                        )
                    }

                    // Physical Testing Guide Dialog
                    if (uiState.showTestingGuide) {
                        TestingGuideDialog(
                            onDismiss = { mainViewModel.setShowTestingGuide(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestMeshPermissions() {
    val permissionList = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionList.add(Manifest.permission.BLUETOOTH_SCAN)
        permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissionList.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
        permissionList.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
    permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
    permissionList.add(Manifest.permission.RECORD_AUDIO)
    permissionList.add(Manifest.permission.CAMERA)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionList.toTypedArray())
    }
}
