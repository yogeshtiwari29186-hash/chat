package com.example.data

import android.content.Context
import android.util.Log
import com.example.crypto.CryptoEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.mesh.MeshTransportManager
import com.example.mesh.model.MeshPacket
import com.example.mesh.model.MeshPeer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

class MeshRepository(
    private val context: Context,
    private val database: AppDatabase,
    val transportManager: MeshTransportManager
) {
    companion object {
        private const val TAG = "MeshRepository"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val userDao = database.userDao()
    val contactDao = database.contactDao()
    val conversationDao = database.conversationDao()
    val messageDao = database.messageDao()
    val groupDao = database.groupDao()
    val relayPacketDao = database.relayPacketDao()
    val statusDao = database.statusDao()
    val advertisementDao = database.advertisementDao()
    val callDao = database.callDao()

    // Current user state
    val currentUser: StateFlow<UserAccountEntity?> = userDao.getUserAccount()
        .stateIn(scope, SharingStarted.Eagerly, null)

    val nearbyPeers: StateFlow<List<MeshPeer>> = transportManager.nearbyPeers
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val contacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val callHistory: Flow<List<CallRecordEntity>> = callDao.getAllCalls()

    init {
        scope.launch {
            // Listen to incoming packets destined for me
            transportManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }

        scope.launch {
            // Listen to relay packets stored for store-and-forward
            transportManager.relayPacketsToStore.collect { packet ->
                val entity = RelayPacketEntity(
                    packetId = packet.packetId,
                    recipientUserId = packet.recipientUserId,
                    serializedPacket = packet.toJson(),
                    hopCount = packet.hopCount,
                    ttl = packet.ttl,
                    expiryTimestamp = packet.expiryTimestamp
                )
                relayPacketDao.insertRelayPacket(entity)
            }
        }
    }

    suspend fun getOrCreateAccount(
        preferredName: String = "You",
        preferredAbout: String = "Online on MeshPulse • Decentralized"
    ): UserAccountEntity {
        val existing = userDao.getUserAccountDirect()
        if (existing != null) {
            transportManager.initialize(existing.userId, existing.displayName, existing.publicKey)
            return existing
        }

        // Generate permanent User ID and cryptographic keypair
        val userId = CryptoEngine.generatePermanentUserId()
        val keyPair = CryptoEngine.generateIdentityKeyPair()
        val recoverySeed = CryptoEngine.generateRecoverySeed()

        val newUser = UserAccountEntity(
            userId = userId,
            displayName = preferredName,
            about = preferredAbout,
            avatarUri = null,
            publicKey = keyPair.publicKeyBase64,
            privateKey = keyPair.privateKeyBase64,
            recoverySeed = recoverySeed
        )
        userDao.insertOrUpdate(newUser)
        transportManager.initialize(newUser.userId, newUser.displayName, newUser.publicKey)

        // Seed initial conversations and contacts for high-fidelity WhatsApp experience
        seedInitialData(newUser)

        return newUser
    }

    private suspend fun seedInitialData(me: UserAccountEntity) {
        val c1 = ContactEntity(
            userId = "MP-7X92-K41P",
            displayName = "Rahul Sharma",
            about = "Testing offline Bluetooth mesh 🚀",
            publicKey = CryptoEngine.generateIdentityKeyPair().publicKeyBase64,
            isNearby = true,
            rssi = -55
        )
        val c2 = ContactEntity(
            userId = "MP-3M81-NQ77",
            displayName = "Priya Patel",
            about = "Relay phone active • Battery 94%",
            publicKey = CryptoEngine.generateIdentityKeyPair().publicKeyBase64,
            isNearby = true,
            rssi = -72
        )
        val c3 = ContactEntity(
            userId = "MP-9L55-VT22",
            displayName = "Aarav Gupta",
            about = "In the basement cafeteria (2 hops)",
            publicKey = CryptoEngine.generateIdentityKeyPair().publicKeyBase64,
            isNearby = false,
            rssi = -89
        )
        contactDao.insertContacts(listOf(c1, c2, c3))

        // Create initial conversations
        val conv1 = ConversationEntity(
            conversationId = "conv_rahul",
            isGroup = false,
            title = "Rahul Sharma",
            lastMessageText = "The encrypted packet routed through Priya's phone smoothly!",
            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 12,
            lastMessageStatus = "READ",
            unreadCount = 0,
            recipientUserId = c1.userId
        )

        val conv2 = ConversationEntity(
            conversationId = "conv_mesh_group",
            isGroup = true,
            title = "Campus Mesh Relay 📡",
            lastMessageText = "Priya: Node B is acting as store-and-forward bridge.",
            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 35,
            lastMessageStatus = "DELIVERED",
            unreadCount = 2,
            groupId = "group_campus_mesh"
        )

        val conv3 = ConversationEntity(
            conversationId = "conv_aarav",
            isGroup = false,
            title = "Aarav Gupta",
            lastMessageText = "Are you reaching me through 2 hops?",
            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
            lastMessageStatus = "DELIVERED",
            unreadCount = 0,
            recipientUserId = c3.userId
        )

        conversationDao.insertOrUpdate(conv1)
        conversationDao.insertOrUpdate(conv2)
        conversationDao.insertOrUpdate(conv3)

        // Seed messages for Rahul
        messageDao.insertMessage(
            MessageEntity(
                messageId = "MSG-INIT-001",
                conversationId = "conv_rahul",
                senderUserId = c1.userId,
                recipientUserId = me.userId,
                content = "Hey! Welcome to MeshPulse. We don't need internet or cellular tower here.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
                deliveryStatus = "READ",
                deliveredTimestamp = System.currentTimeMillis() - 1000 * 60 * 24,
                readTimestamp = System.currentTimeMillis() - 1000 * 60 * 20,
                isIncoming = true
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                messageId = "MSG-INIT-002",
                conversationId = "conv_rahul",
                senderUserId = me.userId,
                recipientUserId = c1.userId,
                content = "Awesome! The store-and-forward mesh is working. Relay nodes can't decrypt our messages right?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 18,
                deliveryStatus = "READ",
                deliveredTimestamp = System.currentTimeMillis() - 1000 * 60 * 17,
                readTimestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                isIncoming = false
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                messageId = "MSG-INIT-003",
                conversationId = "conv_rahul",
                senderUserId = c1.userId,
                recipientUserId = me.userId,
                content = "The encrypted packet routed through Priya's phone smoothly!",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 12,
                deliveryStatus = "READ",
                deliveredTimestamp = System.currentTimeMillis() - 1000 * 60 * 11,
                readTimestamp = System.currentTimeMillis() - 1000 * 60 * 10,
                isIncoming = true
            )
        )

        // Seed Group
        val group = GroupEntity(
            groupId = "group_campus_mesh",
            name = "Campus Mesh Relay 📡",
            description = "Community store-and-forward Bluetooth & Wi-Fi mesh network for offline campus communication.",
            ownerUserId = me.userId,
            inviteCode = "MESH-9921",
            isPublic = true,
            allowsAds = true
        )
        groupDao.insertGroup(group)

        groupDao.insertMember(GroupMemberEntity(groupId = group.groupId, userId = me.userId, displayName = me.displayName, role = "OWNER"))
        groupDao.insertMember(GroupMemberEntity(groupId = group.groupId, userId = c1.userId, displayName = c1.displayName, role = "ADMIN"))
        groupDao.insertMember(GroupMemberEntity(groupId = group.groupId, userId = c2.userId, displayName = c2.displayName, role = "MEMBER"))

        // Seed group messages
        messageDao.insertMessage(
            MessageEntity(
                messageId = "MSG-GRP-001",
                conversationId = "conv_mesh_group",
                senderUserId = c2.userId,
                recipientUserId = "*",
                groupId = group.groupId,
                content = "Priya: Node B is acting as store-and-forward bridge.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 35,
                deliveryStatus = "DELIVERED",
                deliveredTimestamp = System.currentTimeMillis() - 1000 * 60 * 34,
                isIncoming = true
            )
        )

        // Seed Statuses
        statusDao.insertStatus(
            StatusStoryEntity(
                statusId = "STATUS-001",
                userId = c1.userId,
                userName = c1.displayName,
                text = "Mesh network operational at 4 hops! ⚡ Zero cell reception needed.",
                backgroundColor = 0xFF005C4BL,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 45,
                viewsCount = 7
            )
        )
        statusDao.insertStatus(
            StatusStoryEntity(
                statusId = "STATUS-002",
                userId = c2.userId,
                userName = c2.displayName,
                text = "Carrying 14 store-and-forward encrypted packets across campus 🎒📲",
                backgroundColor = 0xFF128C7EL,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 120,
                viewsCount = 12
            )
        )

        // Seed Platform Advertisement
        advertisementDao.insertOrUpdate(
            AdvertisementEntity(
                adId = "AD-PLATFORM-01",
                title = "Emergency Solar Mesh Repeater Kit",
                description = "Self-powered off-grid Bluetooth & LoRa node keeps communication alive during outages.",
                actionUrl = "https://example.com/solar-mesh",
                actionText = "View Equipment",
                impressions = 45,
                clicks = 9,
                isActive = true
            )
        )

        // Seed initial calls
        callDao.insertCall(
            CallRecordEntity(
                callId = "CALL-001",
                peerId = c1.userId,
                peerName = c1.displayName,
                isVideo = false,
                isIncoming = true,
                status = "ANSWERED",
                durationSeconds = 142,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3
            )
        )
        callDao.insertCall(
            CallRecordEntity(
                callId = "CALL-002",
                peerId = c2.userId,
                peerName = c2.displayName,
                isVideo = true,
                isIncoming = false,
                status = "MISSED",
                durationSeconds = 0,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 7
            )
        )
    }

    /**
     * Sends an end-to-end encrypted message.
     */
    suspend fun sendMessage(
        conversationId: String,
        recipientUserId: String,
        content: String,
        mediaUri: String? = null,
        mediaType: String = "NONE",
        replyToMessageId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ): MessageEntity {
        val user = currentUser.value ?: userDao.getUserAccountDirect()!!
        val messageId = CryptoEngine.generateMessageId()

        // 1. Get recipient public key (from contact or generate temporary ephemeral key if new)
        val contact = contactDao.getContactById(recipientUserId)
        val recipientPublicKey = contact?.publicKey?.takeIf { it.isNotBlank() }
            ?: CryptoEngine.generateIdentityKeyPair().publicKeyBase64

        // 2. Encrypt plaintext payload with recipient's public key (intermediate hops cannot decrypt)
        val encryptedPayload = CryptoEngine.encryptForRecipient(content, recipientPublicKey)
        val payloadJson = JSONObject().apply {
            put("ciphertext", encryptedPayload.ciphertextBase64)
            put("iv", encryptedPayload.ivBase64)
            put("ephemeralKey", encryptedPayload.ephemeralPublicKeyBase64)
            put("mediaUri", mediaUri ?: "")
            put("mediaType", mediaType)
        }.toString()

        // 3. Save to local DB with WAITING or RELAYING status
        val isNearby = transportManager.nearbyPeers.value.any { it.userId == recipientUserId }
        val initialStatus = if (isNearby) "RELAYING" else "WAITING"

        val message = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderUserId = user.userId,
            recipientUserId = recipientUserId,
            content = content,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis(),
            deliveryStatus = initialStatus,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            isIncoming = false
        )
        messageDao.insertMessage(message)

        // Update conversation summary
        conversationDao.updateLastMessage(
            id = conversationId,
            lastText = if (mediaType != "NONE") "[$mediaType] $content" else content,
            timestamp = message.timestamp,
            status = initialStatus,
            unreadDelta = 0
        )

        // 4. Wrap into MeshPacket and transmit into the mesh network
        val packet = MeshPacket(
            packetId = messageId,
            type = MeshPacket.PacketType.CHAT_MESSAGE,
            senderUserId = user.userId,
            recipientUserId = recipientUserId,
            encryptedPayload = payloadJson,
            senderPublicKey = user.publicKey,
            timestamp = message.timestamp,
            isMedia = mediaType != "NONE",
            mediaMimeType = mediaType
        )

        scope.launch {
            transportManager.broadcastPacket(packet)

            // Simulate realistic Bluetooth delivery acknowledgement if peer is connected/direct
            if (isNearby) {
                delay(800)
                markMessageDelivered(messageId)
            }
        }

        return message
    }

    /**
     * Processes inbound packets that have reached this node as the final destination.
     */
    private suspend fun handleIncomingPacket(packet: MeshPacket) {
        val user = currentUser.value ?: return

        when (packet.type) {
            MeshPacket.PacketType.CHAT_MESSAGE -> {
                try {
                    val payloadJson = JSONObject(packet.encryptedPayload)
                    val encPayload = CryptoEngine.EncryptedMessagePayload(
                        ciphertextBase64 = payloadJson.getString("ciphertext"),
                        ivBase64 = payloadJson.getString("iv"),
                        ephemeralPublicKeyBase64 = payloadJson.getString("ephemeralKey")
                    )

                    // Decrypt using recipient's private key
                    val decryptedText = CryptoEngine.decryptFromSender(encPayload, user.privateKey)
                    val mediaUri = payloadJson.optString("mediaUri").takeIf { it.isNotBlank() }
                    val mediaType = payloadJson.optString("mediaType", "NONE")

                    val conversationId = "conv_${packet.senderUserId}"

                    // Ensure contact exists
                    var contact = contactDao.getContactById(packet.senderUserId)
                    if (contact == null) {
                        contact = ContactEntity(
                            userId = packet.senderUserId,
                            displayName = "Peer ${packet.senderUserId.takeLast(4)}",
                            publicKey = packet.senderPublicKey,
                            isNearby = true
                        )
                        contactDao.insertOrUpdate(contact)
                    }

                    // Ensure conversation exists
                    var conv = conversationDao.getConversationById(conversationId)
                    if (conv == null) {
                        conv = ConversationEntity(
                            conversationId = conversationId,
                            title = contact.displayName,
                            recipientUserId = packet.senderUserId
                        )
                        conversationDao.insertOrUpdate(conv)
                    }

                    val incomingMsg = MessageEntity(
                        messageId = packet.packetId,
                        conversationId = conversationId,
                        senderUserId = packet.senderUserId,
                        recipientUserId = user.userId,
                        content = decryptedText,
                        mediaUri = mediaUri,
                        mediaType = mediaType,
                        timestamp = packet.timestamp,
                        deliveryStatus = "DELIVERED",
                        deliveredTimestamp = System.currentTimeMillis(),
                        isIncoming = true,
                        hopCount = packet.hopCount
                    )
                    messageDao.insertMessage(incomingMsg)

                    conversationDao.updateLastMessage(
                        id = conversationId,
                        lastText = decryptedText,
                        timestamp = incomingMsg.timestamp,
                        status = "DELIVERED",
                        unreadDelta = 1
                    )

                    // Dispatch delivery ACK packet back to sender
                    val ackPacket = MeshPacket(
                        packetId = "ACK-${packet.packetId}",
                        type = MeshPacket.PacketType.DELIVERY_ACK,
                        senderUserId = user.userId,
                        recipientUserId = packet.senderUserId,
                        encryptedPayload = packet.packetId,
                        senderPublicKey = user.publicKey,
                        timestamp = System.currentTimeMillis()
                    )
                    transportManager.broadcastPacket(ackPacket)

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt incoming message packet ${packet.packetId}", e)
                }
            }

            MeshPacket.PacketType.DELIVERY_ACK -> {
                // The recipient device successfully received and decrypted the packet!
                val targetMsgId = packet.encryptedPayload
                messageDao.updateDeliveryStatus(targetMsgId, "DELIVERED", System.currentTimeMillis())
                Log.i(TAG, "Received Delivery ACK for message: $targetMsgId (Updated to ✓✓)")
            }

            MeshPacket.PacketType.READ_ACK -> {
                // The recipient opened and read the message!
                val targetMsgId = packet.encryptedPayload
                messageDao.updateReadStatus(targetMsgId, System.currentTimeMillis())
                Log.i(TAG, "Received Read ACK for message: $targetMsgId (Updated to Blue ✓✓)")
            }

            else -> {
                // Handled in specific managers (calls, status)
            }
        }
    }

    suspend fun markMessageDelivered(messageId: String) {
        messageDao.updateDeliveryStatus(messageId, "DELIVERED", System.currentTimeMillis())
    }

    suspend fun markConversationRead(conversationId: String) {
        val now = System.currentTimeMillis()
        conversationDao.markAsRead(conversationId)
        messageDao.markIncomingMessagesAsRead(conversationId, now)

        // Send READ_ACK to peers for outgoing read receipt
        val conv = conversationDao.getConversationById(conversationId) ?: return
        val user = currentUser.value ?: return
        if (conv.recipientUserId != null && user.readReceiptsEnabled) {
            val readAckPacket = MeshPacket(
                packetId = "READ-ACK-${System.currentTimeMillis()}",
                type = MeshPacket.PacketType.READ_ACK,
                senderUserId = user.userId,
                recipientUserId = conv.recipientUserId,
                encryptedPayload = conv.conversationId,
                senderPublicKey = user.publicKey,
                timestamp = now
            )
            transportManager.broadcastPacket(readAckPacket)
        }
    }

    suspend fun createStatus(text: String, mediaUri: String?, backgroundColor: Long, privacy: String) {
        val user = currentUser.value ?: return
        val status = StatusStoryEntity(
            statusId = "STATUS-${System.currentTimeMillis()}",
            userId = user.userId,
            userName = user.displayName,
            userAvatarUri = user.avatarUri,
            text = text,
            mediaUri = mediaUri,
            backgroundColor = backgroundColor,
            privacy = privacy
        )
        statusDao.insertStatus(status)
    }

    suspend fun recordStatusView(statusId: String) {
        val user = currentUser.value ?: return
        statusDao.incrementViewCount(statusId)
        statusDao.insertView(
            StatusViewEntity(
                statusId = statusId,
                viewerUserId = user.userId,
                viewerName = user.displayName
            )
        )
    }

    suspend fun logCall(peerId: String, peerName: String, isVideo: Boolean, isIncoming: Boolean, status: String, duration: Int) {
        val call = CallRecordEntity(
            callId = "CALL-${System.currentTimeMillis()}",
            peerId = peerId,
            peerName = peerName,
            isVideo = isVideo,
            isIncoming = isIncoming,
            status = status,
            durationSeconds = duration
        )
        callDao.insertCall(call)
    }
}
