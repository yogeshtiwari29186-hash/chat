package com.example.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MeshRepository
import com.example.data.local.entity.AdvertisementEntity
import com.example.data.local.entity.StatusStoryEntity
import com.example.data.local.entity.StatusViewEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StatusUiState(
    val activeStories: List<StatusStoryEntity> = emptyList(),
    val myStories: List<StatusStoryEntity> = emptyList(),
    val sponsoredAds: List<AdvertisementEntity> = emptyList(),
    val viewingStory: StatusStoryEntity? = null,
    val storyViewers: List<StatusViewEntity> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showAdDetailsDialog: AdvertisementEntity? = null
)

class StatusViewModel(
    private val repository: MeshRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init {
        loadStatuses()
    }

    private fun loadStatuses() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.statusDao.getActiveStatuses(now).collect { stories ->
                val myId = repository.currentUser.value?.userId ?: ""
                val my = stories.filter { it.userId == myId }
                val others = stories.filter { it.userId != myId }
                _uiState.update { it.copy(activeStories = others, myStories = my) }
            }
        }

        viewModelScope.launch {
            repository.advertisementDao.getActiveAds(now).collect { ads ->
                _uiState.update { it.copy(sponsoredAds = ads) }
            }
        }
    }

    fun openStory(story: StatusStoryEntity) {
        _uiState.update { it.copy(viewingStory = story) }
        viewModelScope.launch {
            repository.recordStatusView(story.statusId)
            repository.statusDao.getStatusViews(story.statusId).collect { viewers ->
                _uiState.update { it.copy(storyViewers = viewers) }
            }
        }
    }

    fun closeStory() {
        _uiState.update { it.copy(viewingStory = null, storyViewers = emptyList()) }
    }

    fun setShowCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun createStatus(text: String, mediaUri: String?, backgroundColor: Long, privacy: String) {
        viewModelScope.launch {
            repository.createStatus(text, mediaUri, backgroundColor, privacy)
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun onAdClicked(ad: AdvertisementEntity) {
        viewModelScope.launch {
            repository.advertisementDao.incrementClicks(ad.adId)
            _uiState.update { it.copy(showAdDetailsDialog = ad) }
        }
    }

    fun onAdImpression(ad: AdvertisementEntity) {
        viewModelScope.launch {
            repository.advertisementDao.incrementImpressions(ad.adId)
        }
    }

    fun dismissAdDialog() {
        _uiState.update { it.copy(showAdDetailsDialog = null) }
    }

    class Factory(private val repository: MeshRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatusViewModel(repository) as T
        }
    }
}
