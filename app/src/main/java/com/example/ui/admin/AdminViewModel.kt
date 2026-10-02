package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MeshRepository
import com.example.data.local.entity.AdvertisementEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val authError: String? = null,
    val ads: List<AdvertisementEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val showCreateAdDialog: Boolean = false,
    val selectedTab: AdminTab = AdminTab.ADVERTISEMENTS
)

enum class AdminTab {
    ADVERTISEMENTS,
    USERS,
    GROUPS,
    TELEMETRY
}

class AdminViewModel(
    private val repository: MeshRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.advertisementDao.getAllAdsForAdmin().collect { adList ->
                _uiState.update { it.copy(ads = adList) }
            }
        }
        viewModelScope.launch {
            repository.contactDao.getAllContacts().collect { contactList ->
                _uiState.update { it.copy(contacts = contactList) }
            }
        }
        viewModelScope.launch {
            repository.groupDao.getAllGroups().collect { groupList ->
                _uiState.update { it.copy(groups = groupList) }
            }
        }
    }

    fun verifyPin(pin: String): Boolean {
        // Admin PIN (default 1234 or 9999)
        return if (pin == "1234" || pin == "9999") {
            _uiState.update { it.copy(isAuthenticated = true, authError = null) }
            true
        } else {
            _uiState.update { it.copy(authError = "Invalid Admin PIN. Use 1234.") }
            false
        }
    }

    fun selectTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setShowCreateAdDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateAdDialog = show) }
    }

    fun createAdvertisement(
        title: String,
        description: String,
        actionUrl: String,
        actionText: String,
        isGroupAd: Boolean = false
    ) {
        viewModelScope.launch {
            val ad = AdvertisementEntity(
                adId = "AD-${System.currentTimeMillis()}",
                title = title.trim(),
                description = description.trim(),
                actionUrl = actionUrl.trim(),
                actionText = actionText.ifBlank { "Learn More" },
                isGroupAd = isGroupAd,
                isActive = true
            )
            repository.advertisementDao.insertOrUpdate(ad)
            _uiState.update { it.copy(showCreateAdDialog = false) }
        }
    }

    fun toggleAdActive(ad: AdvertisementEntity) {
        viewModelScope.launch {
            repository.advertisementDao.toggleActive(ad.adId, !ad.isActive)
        }
    }

    fun deleteAd(adId: String) {
        viewModelScope.launch {
            repository.advertisementDao.deleteAd(adId)
        }
    }

    fun toggleBlockUser(contact: ContactEntity) {
        viewModelScope.launch {
            repository.contactDao.setBlocked(contact.userId, !contact.isBlocked)
        }
    }

    fun toggleGroupAds(group: GroupEntity) {
        viewModelScope.launch {
            repository.groupDao.updateGroupAdsSetting(group.groupId, !group.allowsAds)
        }
    }

    class Factory(private val repository: MeshRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(repository) as T
        }
    }
}
