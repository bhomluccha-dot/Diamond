package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserAccount
import com.example.data.model.TokenTransaction
import com.example.data.model.ClaimedCode
import com.example.data.model.CompletedOffer
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class DiamondWalaViewModel(private val repository: AppRepository) : ViewModel() {

    // Database state flows
    val userAccount: StateFlow<UserAccount?> = repository.userAccount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val transactions: StateFlow<List<TokenTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val claimedCodes: StateFlow<List<ClaimedCode>> = repository.allClaimedCodes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedOffers: StateFlow<List<CompletedOffer>> = repository.allCompletedOffers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Ensure initial balances are configured cleanly
    init {
        viewModelScope.launch {
            repository.ensureUserAccountExists()
        }
    }

    // Core earning handlers
    fun earnTokens(amount: Int, source: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val successful = repository.addTokens(amount, source)
            if (successful) {
                onSuccess()
            }
        }
    }

    // Redemptions
    var redemptionStatus by mutableStateOf<String?>(null)
        private set

    fun resetRedemptionStatus() {
        redemptionStatus = null
    }

    fun purchaseGiftCard(itemTitle: String, cost: Int) {
        viewModelScope.launch {
            // Check current balance first to be responsive
            val currentBalance = userAccount.value?.balance ?: 0
            if (currentBalance < cost) {
                redemptionStatus = "INSUFFICIENT"
                return@launch
            }

            // Generate an authentic redeem code
            val prefix = when {
                itemTitle.contains("Google Play", ignoreCase = true) -> "GPLY"
                itemTitle.contains("Steam", ignoreCase = true) -> "STMN"
                itemTitle.contains("App Store", ignoreCase = true) -> "AAPL"
                itemTitle.contains("Free Fire", ignoreCase = true) -> "FFDM"
                else -> "GMRW"
            }
            val randomSegment1 = generateRandomAlphanumeric(4)
            val randomSegment2 = generateRandomAlphanumeric(4)
            val randomSegment3 = generateRandomAlphanumeric(4)
            val redeemCode = "$prefix-$randomSegment1-$randomSegment2-$randomSegment3"

            val success = repository.purchaseRedeemCode(itemTitle, redeemCode, cost)
            if (success) {
                redemptionStatus = "SUCCESS:$redeemCode"
            } else {
                redemptionStatus = "FAILED"
            }
        }
    }

    private fun generateRandomAlphanumeric(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    // Offer / Survey processing
    fun completeOfferwallTask(offerId: String, title: String, type: String, reward: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.completeOffer(offerId, title, type, reward)
            onResult(success)
        }
    }

    // ------------------------------------------------------------------------
    // SPIN WHEEL ENGINE
    // ------------------------------------------------------------------------
    var isSpinning by mutableStateOf(false)
        private set
    var targetRotationAngle by mutableStateOf(0f)
        private set
    var spinResultReward by mutableStateOf<Int?>(null)
        private set
    var spinsRemaining by mutableStateOf(10)
        private set

    fun spinWheel() {
        if (isSpinning || spinsRemaining <= 0) return
        
        isSpinning = true
        spinResultReward = null
        spinsRemaining--

        // Let's create an angle target. 8 segments in the wheel, 45 degrees each.
        // Segments rewards list: 0: Try Again (0 points), 1: +10, 2: +25, 3: +5, 4: +50, 5: Lucky (+100), 6: +15, 7: +30
        val segmentRewards = listOf(0, 10, 25, 5, 50, 100, 15, 30)
        val selectedSegmentIndex = Random.nextInt(8)
        val reward = segmentRewards[selectedSegmentIndex]

        // 360 degrees / 8 segments = 45 degrees.
        // Rotation goes anti-clockwise or clockwise, let's target angle.
        // Plus 5 full rotations (1800 degrees) for dramatic feel.
        val baseRotation = 1800f
        val segmentOffset = selectedSegmentIndex * 45f
        // Add random slight variation so it lands within the segment slice beautifully
        val randomVariation = 5f + Random.nextFloat() * 35f
        targetRotationAngle += baseRotation + (360f - (segmentOffset + randomVariation))

        viewModelScope.launch {
            // Settle after 3.2 seconds
            delay(3200)
            isSpinning = false
            spinResultReward = reward
            if (reward > 0) {
                repository.addTokens(reward, "Diamond Spin Wheel")
            }
        }
    }

    fun resetSpinResult() {
        spinResultReward = null
    }

    // ------------------------------------------------------------------------
    // SCRATCH CARD ENGINE
    // ------------------------------------------------------------------------
    var scratchCardScratchedAmount by mutableStateOf(0) // percentage 0 to 100
        private set
    var isScratchCardRevealed by mutableStateOf(false)
        private set
    var scratchCardReward by mutableStateOf<Int>(0)
        private set
    var scratchGridValues by mutableStateOf(listOf("TRY", "TRY", "TRY"))
        private set

    fun prepareNewScratchCard() {
        scratchCardScratchedAmount = 0
        isScratchCardRevealed = false
        
        // Define random tiers
        // Tier 1: jackpot (3 diamonds) -> 10%
        // Tier 2: double diamond -> 40%
        // Tier 3: single diamond -> 30%
        // Tier 4: blank/retry -> 20%
        val chance = Random.nextInt(100)
        val (reward, values) = when {
            chance < 12 -> Pair(150, listOf("DIAMOND", "DIAMOND", "DIAMOND"))
            chance < 50 -> Pair(40, listOf("DIAMOND", "DIAMOND", "COIN"))
            chance < 80 -> Pair(15, listOf("DIAMOND", "COIN", "TRY"))
            else -> Pair(0, listOf("COIN", "TRY", "TRY"))
        }
        scratchCardReward = reward
        scratchGridValues = values
    }

    fun performScratch(scratchPercentDelta: Int) {
        if (isScratchCardRevealed) return
        scratchCardScratchedAmount = (scratchCardScratchedAmount + scratchPercentDelta).coerceIn(0, 100)
        if (scratchCardScratchedAmount >= 80) {
            revealScratchCard()
        }
    }

    private fun revealScratchCard() {
        if (isScratchCardRevealed) return
        isScratchCardRevealed = true
        scratchCardScratchedAmount = 100
        
        viewModelScope.launch {
            if (scratchCardReward > 0) {
                repository.addTokens(scratchCardReward, "Golden Scratch Card")
            }
        }
    }

    // ------------------------------------------------------------------------
    // MATHEMATICS QUEST GAME
    // ------------------------------------------------------------------------
    var mathQuizValueA by mutableStateOf(0)
        private set
    var mathQuizValueB by mutableStateOf(0)
        private set
    var mathQuizOperation by mutableStateOf("+")
        private set
    var mathQuizChoices by mutableStateOf(listOf<Int>())
        private set
    var mathQuizCorrectAnswer by mutableStateOf(0)
        private set
    var isMathQuizAnswered by mutableStateOf(false)
        private set
    var wasMathQuizCorrect by mutableStateOf<Boolean?>(null)
        private set
    val mathQuizReward = 15

    fun generateNewMathQuiz() {
        isMathQuizAnswered = false
        wasMathQuizCorrect = null
        
        val op = if (Random.nextBoolean()) "+" else "-"
        mathQuizOperation = op
        
        val a = Random.nextInt(10, 99)
        val b = if (op == "+") Random.nextInt(10, 99) else Random.nextInt(5, a)
        mathQuizValueA = a
        mathQuizValueB = b
        
        val correctAnswer = if (op == "+") a + b else a - b
        mathQuizCorrectAnswer = correctAnswer
        
        val choices = mutableSetOf(correctAnswer)
        while (choices.size < 4) {
            val fake = correctAnswer + Random.nextInt(-15, 15)
            if (fake > 0) choices.add(fake)
        }
        mathQuizChoices = choices.toList().shuffled()
    }

    fun submitMathQuizAnswer(selectedChoice: Int) {
        if (isMathQuizAnswered) return
        isMathQuizAnswered = true
        val correct = selectedChoice == mathQuizCorrectAnswer
        wasMathQuizCorrect = correct
        
        if (correct) {
            viewModelScope.launch {
                repository.addTokens(mathQuizReward, "Math Quest Completed")
            }
        }
    }

    // ------------------------------------------------------------------------
    // TREASURE CHEST CLICKER
    // ------------------------------------------------------------------------
    var chestClickProgress by mutableStateOf(0.0f) // 0.0 to 1.0f
        private set
    var chestClicksLeft by mutableStateOf(40)
        private set
    var chestRewardGranted by mutableStateOf<Int?>(null)
        private set
    var secondsLeftChest by mutableStateOf(15)
        private set
    private var isChestActive = false

    fun startChestChallenge() {
        chestClickProgress = 0.0f
        chestClicksLeft = 40
        chestRewardGranted = null
        secondsLeftChest = 15
        isChestActive = true
        
        viewModelScope.launch {
            while (secondsLeftChest > 0 && isChestActive && chestClicksLeft > 0) {
                delay(1000)
                if (chestClicksLeft > 0) {
                    secondsLeftChest--
                }
            }
            if (chestClicksLeft <= 0) {
                // Completed!
                val reward = Random.nextInt(20, 80)
                chestRewardGranted = reward
                repository.addTokens(reward, "Treasure Chest Master")
            } else if (secondsLeftChest == 0) {
                chestRewardGranted = -1 // Failed (Time Out)
            }
            isChestActive = false
        }
    }

    fun clickChest() {
        if (!isChestActive || chestClicksLeft <= 0) return
        chestClicksLeft--
        chestClickProgress = (40f - chestClicksLeft.toFloat()) / 40f
    }
}

class DiamondWalaViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiamondWalaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiamondWalaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
