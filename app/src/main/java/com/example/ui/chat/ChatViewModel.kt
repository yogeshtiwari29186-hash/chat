package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MeshRepository
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.GroupMemberEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageReceiptEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversation: ConversationEntity? = null,
    val group: GroupEntity? = null,
    val groupMembers: List<GroupMemberEntity> = emptyList(),
    val isRecipientNearby: Boolean = false,
    val peerHopCount: Int = 1,
    val replyingToMessage: MessageEntity? = null,
    val selectedMessageForInfo: MessageEntity? = null,
    val messageReceipts: List<MessageReceiptEntity> = emptyList(),
    val showGroupInfo: Boolean = false,
    val isRecordingVoice: Boolean = false
)

class ChatViewModel(
    private val conversationId: String,
    private val repository: MeshRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages: StateFlow<List<MessageEntity>> = repository.messageDao
        .getMessagesForConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadConversation()
        markAsRead()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            val conv = repository.conversationDao.getConversationById(conversationId)
            _uiState.update { it.copy(conversation = conv) }

            if (conv != null && conv.isGroup && conv.groupId != null) {
                val group = repository.groupDao.getGroupById(conv.groupId)
                _uiState.update { it.copy(group = group) }

                repository.groupDao.getMembersForGroup(conv.groupId).collect { members ->
                    _uiState.update { it.copy(groupMembers = members) }
                }
            } else if (conv != null && conv.recipientUserId != null) {
                val isNearby = repository.transportManager.nearbyPeers.value.any { it.userId == conv.recipientUserId }
                _uiState.update { it.copy(isRecipientNearby = isNearby) }
            }
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            repository.markConversationRead(conversationId)
        }
    }

    fun sendMessage(content: String, mediaUri: String? = null, mediaType: String = "NONE") {
        if (content.isBlank() && mediaUri == null) return
        val conv = _uiState.value.conversation ?: return
        val reply = _uiState.value.replyingToMessage

        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conversationId,
                recipientUserId = conv.recipientUserId ?: conv.groupId ?: "*",
                content = content.trim(),
                mediaUri = mediaUri,
                mediaType = mediaType,
                replyToMessageId = reply?.messageId,
                replyToText = reply?.content,
                replyToSender = reply?.senderUserId
            )
            _uiState.update { it.copy(replyingToMessage = null) }
        }
    }

    fun setReplyingTo(message: MessageEntity?) {
        _uiState.update { it.copy(replyingToMessage = message) }
    }

    fun selectMessageForInfo(message: MessageEntity?) {
        _uiState.update { it.copy(selectedMessageForInfo = message) }
        if (message != null) {
            viewModelScope.launch {
                repository.messageDao.getReceiptsForMessage(message.messageId).collect { receipts ->
                    _uiState.update { it.copy(messageReceipts = receipts) }
                }
            }
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.messageDao.deleteMessage(message.messageId)
        }
    }

    fun toggleGroupInfo(show: Boolean) {
        _uiState.update { it.copy(showGroupInfo = show) }
    }

    fun addGroupMember(userId: String, displayName: String) {
        val group = _uiState.value.group ?: return
        if (userId.isBlank() || displayName.isBlank()) return
        viewModelScope.launch {
            repository.groupDao.insertMember(
                GroupMemberEntity(
                    groupId = group.groupId,
                    userId = userId.trim(),
                    displayName = displayName.trim(),
                    role = "MEMBER"
                )
            )
        }
    }

    fun removeGroupMember(userId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.removeMember(group.groupId, userId) }
    }

    fun promoteGroupAdmin(userId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.updateMemberRole(group.groupId, userId, "ADMIN") }
    }

    fun setOnlyAdminsCanEditInfo(value: Boolean) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.setOnlyAdminsCanEditInfo(group.groupId, value) }
    }

    fun setOnlyAdminsCanSendMessages(value: Boolean) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.setOnlyAdminsCanSendMessages(group.groupId, value) }
    }

    fun setApprovalRequired(value: Boolean) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.setApprovalRequired(group.groupId, value) }
    }

    fun setInviteLinkEnabled(value: Boolean) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.setInviteLinkEnabled(group.groupId, value) }
    }

    fun setDisappearingMessages(seconds: Long) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch { repository.groupDao.setDisappearingMessages(group.groupId, seconds) }
    }

    fun toggleGroupAds(allow: Boolean) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            repository.groupDao.updateGroupAdsSetting(group.groupId, allow)
            _uiState.update { it.copy(group = group.copy(allowsAds = allow)) }
        }
    }

    class Factory(
        private val conversationId: String,
        private val repository: MeshRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(conversationId, repository) as T
        }
    }
}
