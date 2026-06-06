package com.example.data.repository

import com.example.data.database.daos.AppDao
import com.example.data.model.UserAccount
import com.example.data.model.TokenTransaction
import com.example.data.model.ClaimedCode
import com.example.data.model.CompletedOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val appDao: AppDao) {

    val userAccount: Flow<UserAccount?> = appDao.getUserAccount()
    val allTransactions: Flow<List<TokenTransaction>> = appDao.getAllTransactions()
    val allClaimedCodes: Flow<List<ClaimedCode>> = appDao.getAllClaimedCodes()
    val allCompletedOffers: Flow<List<CompletedOffer>> = appDao.getAllCompletedOffers()

    suspend fun ensureUserAccountExists() = withContext(Dispatchers.IO) {
        val existing = appDao.getUserAccountDirect()
        if (existing == null) {
            val defaultAccount = UserAccount(
                id = 1,
                username = "GoldenGamer_" + (1000..9999).random(),
                balance = 150, // Premium warm dynamic user welcoming (150 initial gold coins!)
                level = 1,
                totalEarned = 150
            )
            appDao.insertUserAccount(defaultAccount)
            appDao.insertTransaction(
                TokenTransaction(
                    type = "EARN",
                    source = "Welcome Bonus",
                    amount = 150
                )
            )
        }
    }

    suspend fun addTokens(amount: Int, source: String): Boolean = withContext(Dispatchers.IO) {
        if (amount <= 0) return@withContext false
        val currentAccount = appDao.getUserAccountDirect() ?: return@withContext false
        
        val newBalance = currentAccount.balance + amount
        val newTotal = currentAccount.totalEarned + amount
        // Simple levels system: e.g., level increases every 1000 total tokens earned
        val calculatedLevel = (newTotal / 1000) + 1
        val updatedAccount = currentAccount.copy(
            balance = newBalance,
            totalEarned = newTotal,
            level = if (calculatedLevel > currentAccount.level) calculatedLevel else currentAccount.level
        )
        
        appDao.updateUserAccount(updatedAccount)
        appDao.insertTransaction(
            TokenTransaction(
                type = "EARN",
                source = source,
                amount = amount
            )
        )
        true
    }

    suspend fun purchaseRedeemCode(itemTitle: String, code: String, cost: Int): Boolean = withContext(Dispatchers.IO) {
        if (cost <= 0) return@withContext false
        val currentAccount = appDao.getUserAccountDirect() ?: return@withContext false
        
        if (currentAccount.balance < cost) {
            return@withContext false // Insufficient tokens
        }
        
        val updatedAccount = currentAccount.copy(balance = currentAccount.balance - cost)
        appDao.updateUserAccount(updatedAccount)
        
        // Write transaction record
        appDao.insertTransaction(
            TokenTransaction(
                type = "SPEND",
                source = "Redeemed $itemTitle",
                amount = cost
            )
        )
        
        // Store claimed code
        appDao.insertClaimedCode(
            ClaimedCode(
                itemTitle = itemTitle,
                redeemCode = code,
                costInTokens = cost
            )
        )
        true
    }

    suspend fun completeOffer(offerId: String, title: String, type: String, reward: Int): Boolean = withContext(Dispatchers.IO) {
        val alreadyDone = appDao.isOfferCompleted(offerId)
        if (alreadyDone) return@withContext false
        
        // Record completed offer
        appDao.insertCompletedOffer(
            CompletedOffer(
                offerId = offerId,
                title = title,
                type = type,
                reward = reward
            )
        )
        // Add tokens
        addTokens(reward, "Offer: $title")
        true
    }
}
