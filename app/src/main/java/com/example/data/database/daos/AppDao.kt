package com.example.data.database.daos

import androidx.room.*
import com.example.data.model.UserAccount
import com.example.data.model.TokenTransaction
import com.example.data.model.ClaimedCode
import com.example.data.model.CompletedOffer
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM user_account WHERE id = 1 LIMIT 1")
    fun getUserAccount(): Flow<UserAccount?>

    @Query("SELECT * FROM user_account WHERE id = 1 LIMIT 1")
    suspend fun getUserAccountDirect(): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(account: UserAccount)

    @Update
    suspend fun updateUserAccount(account: UserAccount)

    @Query("SELECT * FROM token_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TokenTransaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TokenTransaction)

    @Query("SELECT * FROM claimed_codes ORDER BY timestamp DESC")
    fun getAllClaimedCodes(): Flow<List<ClaimedCode>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClaimedCode(code: ClaimedCode)

    @Query("SELECT * FROM completed_offers ORDER BY completedAt DESC")
    fun getAllCompletedOffers(): Flow<List<CompletedOffer>>

    @Query("SELECT EXISTS(SELECT 1 FROM completed_offers WHERE offerId = :offerId LIMIT 1)")
    suspend fun isOfferCompleted(offerId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedOffer(offer: CompletedOffer)
}
