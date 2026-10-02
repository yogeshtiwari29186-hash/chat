package com.example.calling

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.data.MeshRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveCallState(
    val callId: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val peerAvatarUri: String? = null,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val status: CallStatus = CallStatus.IDLE,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val durationSeconds: Int = 0,
    val isDirectLowLatency: Boolean = true, // 1-hop direct peer vs multi-hop
    val warningMessage: String? = null
) {
    enum class CallStatus {
        IDLE,
        OUTGOING_CALLING,
        INCOMING_RINGING,
        CONNECTED,
        ENDED
    }
}

class CallManager(
    private val context: Context,
    private val repository: MeshRepository
) {
    companion object {
        private const val TAG = "CallManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _callState = MutableStateFlow(ActiveCallState())
    val callState: StateFlow<ActiveCallState> = _callState.asStateFlow()

    private var durationJob: Job? = null

    fun startOutgoingCall(peerId: String, peerName: String, isVideo: Boolean, isDirect: Boolean = true) {
        val warning = if (!isDirect) {
            "Warning: Calling via multi-hop relay. Real-time media may have high jitter or packet loss."
        } else null

        _callState.value = ActiveCallState(
            callId = "CALL-${System.currentTimeMillis()}",
            peerId = peerId,
            peerName = peerName,
            isVideo = isVideo,
            isIncoming = false,
            status = ActiveCallState.CallStatus.OUTGOING_CALLING,
            isDirectLowLatency = isDirect,
            warningMessage = warning
        )

        // Simulate peer ringing and connection pickup for testability
        scope.launch {
            delay(2500)
            if (_callState.value.status == ActiveCallState.CallStatus.OUTGOING_CALLING) {
                connectCall()
            }
        }
    }

    fun triggerIncomingCall(peerId: String, peerName: String, isVideo: Boolean, isDirect: Boolean = true) {
        _callState.value = ActiveCallState(
            callId = "CALL-${System.currentTimeMillis()}",
            peerId = peerId,
            peerName = peerName,
            isVideo = isVideo,
            isIncoming = true,
            status = ActiveCallState.CallStatus.INCOMING_RINGING,
            isDirectLowLatency = isDirect
        )
    }

    fun acceptIncomingCall() {
        connectCall()
    }

    fun declineIncomingCall() {
        val current = _callState.value
        scope.launch {
            repository.logCall(
                peerId = current.peerId,
                peerName = current.peerName,
                isVideo = current.isVideo,
                isIncoming = true,
                status = "DECLINED",
                duration = 0
            )
        }
        _callState.value = _callState.value.copy(status = ActiveCallState.CallStatus.ENDED)
        resetCallDelayed()
    }

    private fun connectCall() {
        _callState.value = _callState.value.copy(
            status = ActiveCallState.CallStatus.CONNECTED,
            durationSeconds = 0
        )

        durationJob?.cancel()
        durationJob = scope.launch {
            while (_callState.value.status == ActiveCallState.CallStatus.CONNECTED) {
                delay(1000)
                _callState.value = _callState.value.copy(
                    durationSeconds = _callState.value.durationSeconds + 1
                )
            }
        }
    }

    fun toggleMute() {
        val newMute = !_callState.value.isMuted
        _callState.value = _callState.value.copy(isMuted = newMute)
    }

    fun toggleCamera() {
        val newCam = !_callState.value.isCameraOn
        _callState.value = _callState.value.copy(isCameraOn = newCam)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_callState.value.isSpeakerOn
        audioManager?.isSpeakerphoneOn = newSpeaker
        _callState.value = _callState.value.copy(isSpeakerOn = newSpeaker)
    }

    fun endCall() {
        val current = _callState.value
        durationJob?.cancel()

        scope.launch {
            repository.logCall(
                peerId = current.peerId,
                peerName = current.peerName,
                isVideo = current.isVideo,
                isIncoming = current.isIncoming,
                status = "ANSWERED",
                duration = current.durationSeconds
            )
        }

        _callState.value = current.copy(status = ActiveCallState.CallStatus.ENDED)
        resetCallDelayed()
    }

    private fun resetCallDelayed() {
        scope.launch {
            delay(1200)
            _callState.value = ActiveCallState()
        }
    }
}
