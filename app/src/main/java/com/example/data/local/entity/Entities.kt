package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val about: String = "Using MeshPulse • Private & Offline",
    val avatarUri: String? = null,
    val publicKey: String,
    val privateKey: String,
    val recoverySeed: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenVisibility: String = "EVERYONE",
    val readReceiptsEnabled: Boolean = true,
    val profilePhotoVisibility: String = "EVERYONE",
    val statusVisibility: String = "CONTACTS",
    val allowGroupInvites: String = "EVERYONE"
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val about: String = "",
    val avatarUri: String? = null,
    val publicKey: String = "",
    val isNearby: Boolean = false,
    val rssi: Int = -80,
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val isGroup: Boolean = false,
    val title: String,
    val avatarUri: String? = null,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val lastMessageStatus: String = "DELIVERED",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val recipientUserId: String? = null,
    val groupId: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderUserId: String,
    val recipientUserId: String,
    val groupId: String? = null,
    val content: String,
    val mediaUri: String? = null,
    val mediaType: String = "NONE", // NONE, IMAGE, VIDEO, AUDIO, VOICE_NOTE, DOCUMENT
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryStatus: String = "WAITING", // WAITING, RELAYING, DELIVERED, READ, FAILED
    val deliveredTimestamp: Long? = null,
    val readTimestamp: Long? = null,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val isIncoming: Boolean = false,
    val hopCount: Int = 0
)

@Entity(tableName = "message_receipts")
data class MessageReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val userId: String,
    val userName: String,
    val deliveredTimestamp: Long? = null,
    val readTimestamp: Long? = null
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val name: String,
    val description: String = "",
    val avatarUri: String? = null,
    val ownerUserId: String,
    val inviteCode: String,
    val isPublic: Boolean = false,
    val allowsAds: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
    val displayName: String,
    val avatarUri: String? = null,
    val role: String = "MEMBER", // OWNER, ADMIN, MEMBER
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "relay_packets")
data class RelayPacketEntity(
    @PrimaryKey val packetId: String,
    val recipientUserId: String,
    val serializedPacket: String,
    val hopCount: Int = 0,
    val ttl: Int = 6,
    val expiryTimestamp: Long,
    val status: String = "STORED" // STORED, FORWARDING, FORWARDED, EXPIRED
)

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey val statusId: String,
    val userId: String,
    val userName: String,
    val userAvatarUri: String? = null,
    val text: String = "",
    val mediaUri: String? = null,
    val mediaType: String = "TEXT", // TEXT, IMAGE, VIDEO
    val backgroundColor: Long = 0xFF005C4BL,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val viewsCount: Int = 0,
    val privacy: String = "CONTACTS" // EVERYONE, CONTACTS, SELECTED
)

@Entity(tableName = "status_views")
data class StatusViewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val statusId: String,
    val viewerUserId: String,
    val viewerName: String,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "advertisements")
data class AdvertisementEntity(
    @PrimaryKey val adId: String,
    val title: String,
    val description: String,
    val mediaUri: String? = null,
    val actionUrl: String? = null,
    val actionText: String = "Learn More",
    val impressions: Int = 0,
    val clicks: Int = 0,
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L,
    val isGroupAd: Boolean = false,
    val groupId: String? = null
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val callId: String,
    val peerId: String,
    val peerName: String,
    val peerAvatarUri: String? = null,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val status: String = "ANSWERED", // ANSWERED, MISSED, DECLINED
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
