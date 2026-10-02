package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_account LIMIT 1")
    fun getUserAccount(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_account LIMIT 1")
    suspend fun getUserAccountDirect(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccountEntity)

    @Query("UPDATE user_account SET displayName = :name, about = :about, avatarUri = :avatarUri WHERE userId = :userId")
    suspend fun updateProfile(userId: String, name: String, about: String, avatarUri: String?)

    @Query("DELETE FROM user_account")
    suspend fun clearUser()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId LIMIT 1")
    suspend fun getContactById(userId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE isNearby = 1 ORDER BY rssi DESC")
    fun getNearbyContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET isNearby = :isNearby, rssi = :rssi, lastSeenTimestamp = :lastSeen WHERE userId = :userId")
    suspend fun updateNearbyStatus(userId: String, isNearby: Boolean, rssi: Int, lastSeen: Long)

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE userId = :userId")
    suspend fun setBlocked(userId: String, isBlocked: Boolean)

    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun deleteContact(userId: String)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessageText = :lastText, lastMessageTimestamp = :timestamp, lastMessageStatus = :status, unreadCount = unreadCount + :unreadDelta WHERE conversationId = :id")
    suspend fun updateLastMessage(id: String, lastText: String, timestamp: Long, status: String, unreadDelta: Int)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun deleteConversation(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET deliveryStatus = :status, deliveredTimestamp = :deliveredTime WHERE messageId = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: String, deliveredTime: Long?)

    @Query("UPDATE messages SET deliveryStatus = 'READ', readTimestamp = :readTime WHERE messageId = :messageId")
    suspend fun updateReadStatus(messageId: String, readTime: Long)

    @Query("UPDATE messages SET deliveryStatus = 'READ', readTimestamp = :readTime WHERE conversationId = :convId AND isIncoming = 1 AND deliveryStatus != 'READ'")
    suspend fun markIncomingMessagesAsRead(convId: String, readTime: Long)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteConversationMessages(convId: String)

    // Message Receipts for group read info
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: MessageReceiptEntity)

    @Query("SELECT * FROM message_receipts WHERE messageId = :messageId")
    fun getReceiptsForMessage(messageId: String): Flow<List<MessageReceiptEntity>>
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: String): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: String, userId: String)

    @Query("UPDATE group_members SET role = :role WHERE groupId = :groupId AND userId = :userId")
    suspend fun updateMemberRole(groupId: String, userId: String, role: String)

    @Query("UPDATE groups SET name = :name, description = :description, updatedAt = :updatedAt WHERE groupId = :groupId")
    suspend fun updateGroupInfo(groupId: String, name: String, description: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET allowsAds = :allowAds WHERE groupId = :groupId")
    suspend fun updateGroupAdsSetting(groupId: String, allowAds: Boolean)

    @Query("UPDATE groups SET onlyAdminsCanEditInfo = :value WHERE groupId = :groupId")
    suspend fun setOnlyAdminsCanEditInfo(groupId: String, value: Boolean)

    @Query("UPDATE groups SET onlyAdminsCanSendMessages = :value WHERE groupId = :groupId")
    suspend fun setOnlyAdminsCanSendMessages(groupId: String, value: Boolean)

    @Query("UPDATE groups SET disappearingMessagesSeconds = :seconds WHERE groupId = :groupId")
    suspend fun setDisappearingMessages(groupId: String, seconds: Long)

    @Query("UPDATE groups SET approvalRequired = :value WHERE groupId = :groupId")
    suspend fun setApprovalRequired(groupId: String, value: Boolean)

    @Query("UPDATE groups SET inviteLinkEnabled = :value WHERE groupId = :groupId")
    suspend fun setInviteLinkEnabled(groupId: String, value: Boolean)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getMember(groupId: String, userId: String): GroupMemberEntity?

    @Query("DELETE FROM groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)
}

@Dao
interface RelayPacketDao {
    @Query("SELECT * FROM relay_packets WHERE status = 'STORED' AND expiryTimestamp > :currentTime ORDER BY hopCount ASC, expiryTimestamp ASC")
    suspend fun getPendingRelayPackets(currentTime: Long): List<RelayPacketEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelayPacket(packet: RelayPacketEntity): Long

    @Query("UPDATE relay_packets SET status = :status WHERE packetId = :packetId")
    suspend fun updatePacketStatus(packetId: String, status: String)

    @Query("DELETE FROM relay_packets WHERE expiryTimestamp <= :currentTime OR status = 'FORWARDED'")
    suspend fun purgeExpiredPackets(currentTime: Long)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM status_stories WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getActiveStatuses(currentTime: Long): Flow<List<StatusStoryEntity>>

    @Query("SELECT * FROM status_stories WHERE userId = :userId AND expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getMyStatuses(userId: String, currentTime: Long): Flow<List<StatusStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusStoryEntity)

    @Query("UPDATE status_stories SET viewsCount = viewsCount + 1 WHERE statusId = :statusId")
    suspend fun incrementViewCount(statusId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertView(view: StatusViewEntity)

    @Query("SELECT * FROM status_views WHERE statusId = :statusId ORDER BY viewedAt DESC")
    fun getStatusViews(statusId: String): Flow<List<StatusViewEntity>>

    @Query("DELETE FROM status_stories WHERE expiresAt <= :currentTime")
    suspend fun purgeExpiredStatuses(currentTime: Long)
}

@Dao
interface AdvertisementDao {
    @Query("SELECT * FROM advertisements WHERE isActive = 1 AND startDate <= :now AND endDate >= :now ORDER BY impressions ASC")
    fun getActiveAds(now: Long): Flow<List<AdvertisementEntity>>

    @Query("SELECT * FROM advertisements ORDER BY startDate DESC")
    fun getAllAdsForAdmin(): Flow<List<AdvertisementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(ad: AdvertisementEntity)

    @Query("UPDATE advertisements SET impressions = impressions + 1 WHERE adId = :adId")
    suspend fun incrementImpressions(adId: String)

    @Query("UPDATE advertisements SET clicks = clicks + 1 WHERE adId = :adId")
    suspend fun incrementClicks(adId: String)

    @Query("UPDATE advertisements SET isActive = :isActive WHERE adId = :adId")
    suspend fun toggleActive(adId: String, isActive: Boolean)

    @Query("DELETE FROM advertisements WHERE adId = :adId")
    suspend fun deleteAd(adId: String)
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallRecordEntity)

    @Query("DELETE FROM call_records WHERE callId = :callId")
    suspend fun deleteCall(callId: String)

    @Query("DELETE FROM call_records")
    suspend fun clearHistory()
}
