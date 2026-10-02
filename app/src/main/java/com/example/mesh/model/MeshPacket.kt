package com.example.mesh.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * The fundamental wire-format envelope for store-and-forward mesh routing.
 *
 * PRIVACY CONTRACT:
 * - Only routing metadata (senderId, recipientId, packetId, ttl, timestamps) is visible
 *   to intermediate relay phones.
 * - The `encryptedPayload` contains the AES-GCM ciphertext which ONLY the intended
 *   recipient can decrypt with their private key.
 * - Intermediate relays do NOT have the keys to decrypt.
 */
data class MeshPacket(
    val packetId: String,
    val type: PacketType,
    val senderUserId: String,
    val recipientUserId: String,
    val encryptedPayload: String, // Ciphertext + IV + Ephemeral key
    val senderPublicKey: String,
    val timestamp: Long,
    val ttl: Int = DEFAULT_TTL,
    val hopCount: Int = 0,
    val expiryTimestamp: Long = timestamp + DEFAULT_EXPIRY_MS,
    val visitedNodeIds: List<String> = emptyList(),
    val isMedia: Boolean = false,
    val mediaMimeType: String = ""
) {
    enum class PacketType {
        CHAT_MESSAGE,
        DELIVERY_ACK,
        READ_ACK,
        STATUS_BROADCAST,
        CALL_SIGNAL,
        PEER_HANDSHAKE
    }

    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime > expiryTimestamp || ttl <= 0
    }

    fun withHop(currentNodeId: String): MeshPacket {
        val updatedVisited = visitedNodeIds.toMutableList()
        if (!updatedVisited.contains(currentNodeId)) {
            updatedVisited.add(currentNodeId)
        }
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1,
            visitedNodeIds = updatedVisited
        )
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("packetId", packetId)
        json.put("type", type.name)
        json.put("senderUserId", senderUserId)
        json.put("recipientUserId", recipientUserId)
        json.put("encryptedPayload", encryptedPayload)
        json.put("senderPublicKey", senderPublicKey)
        json.put("timestamp", timestamp)
        json.put("ttl", ttl)
        json.put("hopCount", hopCount)
        json.put("expiryTimestamp", expiryTimestamp)
        json.put("isMedia", isMedia)
        json.put("mediaMimeType", mediaMimeType)

        val array = JSONArray()
        visitedNodeIds.forEach { array.put(it) }
        json.put("visitedNodeIds", array)
        return json.toString()
    }

    companion object {
        const val DEFAULT_TTL = 6
        const val DEFAULT_EXPIRY_MS = 3 * 24 * 60 * 60 * 1000L // 3 days store-and-forward

        fun fromJson(jsonStr: String): MeshPacket? {
            return try {
                val json = JSONObject(jsonStr)
                val visitedArray = json.optJSONArray("visitedNodeIds")
                val visitedList = mutableListOf<String>()
                if (visitedArray != null) {
                    for (i in 0 until visitedArray.length()) {
                        visitedList.add(visitedArray.getString(i))
                    }
                }

                MeshPacket(
                    packetId = json.getString("packetId"),
                    type = PacketType.valueOf(json.getString("type")),
                    senderUserId = json.getString("senderUserId"),
                    recipientUserId = json.getString("recipientUserId"),
                    encryptedPayload = json.getString("encryptedPayload"),
                    senderPublicKey = json.optString("senderPublicKey", ""),
                    timestamp = json.getLong("timestamp"),
                    ttl = json.optInt("ttl", DEFAULT_TTL),
                    hopCount = json.optInt("hopCount", 0),
                    expiryTimestamp = json.optLong("expiryTimestamp", System.currentTimeMillis() + DEFAULT_EXPIRY_MS),
                    visitedNodeIds = visitedList,
                    isMedia = json.optBoolean("isMedia", false),
                    mediaMimeType = json.optString("mediaMimeType", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
