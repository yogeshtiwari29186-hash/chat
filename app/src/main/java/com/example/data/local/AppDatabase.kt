package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        UserAccountEntity::class,
        ContactEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MessageReceiptEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        RelayPacketEntity::class,
        StatusStoryEntity::class,
        StatusViewEntity::class,
        AdvertisementEntity::class,
        CallRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun relayPacketDao(): RelayPacketDao
    abstract fun statusDao(): StatusDao
    abstract fun advertisementDao(): AdvertisementDao
    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meshpulse_encrypted.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
