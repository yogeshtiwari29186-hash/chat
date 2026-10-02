package com.example.mesh.model

/**
 * Represents a nearby peer discovered over Bluetooth LE, Bluetooth Classic, or Wi-Fi Direct.
 */
data class MeshPeer(
    val deviceId: String,             // MAC or simulated hardware address
    val userId: String? = null,       // Permanent MeshPulse User ID (e.g. MP-XXXX-XXXX)
    val displayName: String,          // Display name advertised or exchanged
    val about: String = "",
    val avatarUri: String? = null,
    val publicKey: String? = null,    // Public identity key
    val rssi: Int = -70,              // Signal strength in dBm (-40 is strong, -95 is weak)
    val connectionState: PeerConnectionState = PeerConnectionState.DISCOVERED,
    val isDirect: Boolean = true,     // Direct radio peer or multi-hop reachable
    val hopDistance: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    enum class PeerConnectionState {
        DISCOVERED,
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    val signalQualityPercent: Int
        get() {
            // Map -100 dBm (0%) to -45 dBm (100%)
            val clamped = rssi.coerceIn(-100, -45)
            return ((clamped + 100) * 100) / 55
        }
}
