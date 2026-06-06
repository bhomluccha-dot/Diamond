package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey val id: Int = 1,
    val username: String = "LuckyGamer",
    val balance: Int = 100, // Starts with 100 welcome bonus tokens
    val level: Int = 1,
    val totalEarned: Int = 100
)

@Entity(tableName = "token_transactions")
data class TokenTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "EARN" or "SPEND"
    val source: String, // e.g., "Spin Wheel", "Scratch Card", "Math Quiz", "Completed Survey"
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "claimed_codes")
data class ClaimedCode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemTitle: String, // e.g. "Google Play $5 code"
    val redeemCode: String, // e.g. "PLAY-ABCD-1234-XYZ9"
    val costInTokens: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "completed_offers")
data class CompletedOffer(
    @PrimaryKey val offerId: String,
    val title: String,
    val type: String, // "SURVEY" or "TASK"
    val reward: Int,
    val completedAt: Long = System.currentTimeMillis()
)
