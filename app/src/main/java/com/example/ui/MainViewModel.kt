package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calling.CallManager
import com.example.data.MeshRepository
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.mesh.model.MeshPeer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab {
    CHATS,
    STATUS,
    CALLS,
    MESH_PEERS
}

data class MainUiState(
    val isLoading: Boolean = true,
    val isAccountSetup: Boolean = false,
    val currentUser: UserAccountEntity? = null,
    val selectedTab: MainTab = MainTab.CHATS,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showAdminDashboard: Boolean = false,
    val showProfileSheet: Boolean = false,
    val showNewChatDialog: Boolean = false,
    val showNewGroupDialog: Boolean = false,
    val showTestingGuide: Boolean = false
)

class MainViewModel(
    private val repository: MeshRepository,
    val callManager: CallManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nearbyPeers: StateFlow<List<MeshPeer>> = repository.nearbyPeers

    val callHistory = repository.callHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkExistingAccount()
    }

    private fun checkExistingAccount() {
        viewModelScope.launch {
            val user = repository.userDao.getUserAccountDirect()
            if (user != null) {
                repository.transportManager.initialize(user.userId, user.displayName, user.publicKey)
                _uiState.update { it.copy(isLoading = false, isAccountSetup = true, currentUser = user) }
            } else {
                _uiState.update { it.copy(isLoading = false, isAccountSetup = false) }
            }
        }
    }

    fun completeAccountCreation(name: String, about: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newUser = repository.getOrCreateAccount(name, about)
            _uiState.update {
                it.copy(isLoading = false, isAccountSetup = true, currentUser = newUser)
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active, searchQuery = if (!active) "" else it.searchQuery) }
    }

    fun setShowAdminDashboard(show: Boolean) {
        _uiState.update { it.copy(showAdminDashboard = show) }
    }

    fun setShowProfileSheet(show: Boolean) {
        _uiState.update { it.copy(showProfileSheet = show) }
    }

    fun setShowNewChatDialog(show: Boolean) {
        _uiState.update { it.copy(showNewChatDialog = show) }
    }

    fun setShowNewGroupDialog(show: Boolean) {
        _uiState.update { it.copy(showNewGroupDialog = show) }
    }

    fun setShowTestingGuide(show: Boolean) {
        _uiState.update { it.copy(showTestingGuide = show) }
    }

    fun startVoiceCall(peerId: String, peerName: String, isDirect: Boolean = true) {
        callManager.startOutgoingCall(peerId, peerName, isVideo = false, isDirect = isDirect)
    }

    fun startVideoCall(peerId: String, peerName: String, isDirect: Boolean = true) {
        callManager.startOutgoingCall(peerId, peerName, isVideo = true, isDirect = isDirect)
    }

    fun createNewDirectChat(peerUserId: String, peerName: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val convId = "conv_$peerUserId"
            val existing = repository.conversationDao.getConversationById(convId)
            if (existing == null) {
                repository.conversationDao.insertOrUpdate(
                    ConversationEntity(
                        conversationId = convId,
                        isGroup = false,
                        title = peerName,
                        recipientUserId = peerUserId,
                        lastMessageText = "Started encrypted chat",
                        lastMessageTimestamp = System.currentTimeMillis()
                    )
                )
            }
            onCreated(convId)
        }
    }

    fun createGroup(name: String, description: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val user = _uiState.value.currentUser ?: return@launch
            val groupId = "group_${System.currentTimeMillis()}"
            val group = com.example.data.local.entity.GroupEntity(
                groupId = groupId,
                name = name,
                description = description,
                ownerUserId = user.userId,
                inviteCode = "MESH-${(1000..9999).random()}",
                isPublic = true,
                allowsAds = true
            )
            repository.groupDao.insertGroup(group)
            repository.groupDao.insertMember(
                com.example.data.local.entity.GroupMemberEntity(
                    groupId = groupId,
                    userId = user.userId,
                    displayName = user.displayName,
                    role = "OWNER"
                )
            )

            val convId = "conv_$groupId"
            repository.conversationDao.insertOrUpdate(
                ConversationEntity(
                    conversationId = convId,
                    isGroup = true,
                    title = name,
                    groupId = groupId,
                    lastMessageText = "Group created",
                    lastMessageTimestamp = System.currentTimeMillis()
                )
            )
            onCreated(convId)
        }
    }

    fun updateProfile(name: String, about: String, avatarUri: String?) {
        viewModelScope.launch {
            val current = _uiState.value.currentUser ?: return@launch
            repository.userDao.updateProfile(current.userId, name, about, avatarUri)
            repository.transportManager.updateProfile(name)
            val updated = current.copy(displayName = name, about = about, avatarUri = avatarUri)
            _uiState.update { it.copy(currentUser = updated) }
        }
    }

    class Factory(
        private val repository: MeshRepository,
        private val callManager: CallManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, callManager) as T
        }
    }
}
