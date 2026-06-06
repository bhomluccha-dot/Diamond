package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DiamondWalaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class TabItem(val route: String, val title: String) {
    GAMES("games", "Arcade"),
    TASKS("tasks", "Offerwall"),
    REDEEM("redeem", "Redeem"),
    HISTORY("history", "My Vault")
}

// Mini-game designations
enum class ActiveGameType {
    NONE,
    SPIN_WHEEL,
    SCRATCH_CARD,
    MATH_QUEST,
    CHEST_CLICKER,
    ARCADE_SIMULATOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: DiamondWalaViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Base database states
    val user by viewModel.userAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val claimedCodes by viewModel.claimedCodes.collectAsStateWithLifecycle()
    val completedOffers by viewModel.completedOffers.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(TabItem.GAMES) }
    var activeMiniGame by remember { mutableStateOf(ActiveGameType.NONE) }
    var selectedArcadeGameName by remember { mutableStateOf("") }
    
    // Dialog / Modal Triggers
    var showRedeemConfirmation by remember { mutableStateOf<GiftCardOption?>(null) }
    var activeSurveyModal by remember { mutableStateOf<SurveyMock?>(null) }
    var activeTaskVideoModal by remember { mutableStateOf<TaskMock?>(null) }
    var showTransactionLedger by remember { mutableStateOf(false) }

    // Intercept redemption state from view model
    val redemptionStatus = viewModel.redemptionStatus
    if (redemptionStatus != null) {
        if (redemptionStatus.startsWith("SUCCESS:")) {
            val code = redemptionStatus.substringAfter("SUCCESS:")
            Dialog(onDismissRequest = { viewModel.resetRedemptionStatus() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("success_redeem_dialog"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(GreenSuccess.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = GreenSuccess,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "CLAIMED SUCCESSFULLY!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = GreenSuccess,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your digital voucher has been processed and ready. Tap copy to redeem instantly in your account.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Render copyable code card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryCyan
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(code))
                                        Toast.makeText(context, "Redeem Code Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_button_dialog")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Copy code",
                                        tint = SecondaryGold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.resetRedemptionStatus() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            Text("Awesome", color = ThemeBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (redemptionStatus == "INSUFFICIENT") {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "Insufficient diamonds! Play more games to earn.", Toast.LENGTH_LONG).show()
                viewModel.resetRedemptionStatus()
            }
        } else if (redemptionStatus == "FAILED") {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "Transaction failed. Please try again.", Toast.LENGTH_LONG).show()
                viewModel.resetRedemptionStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(ThemeBackground)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(PrimaryCyan, Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.5.dp, PrimaryCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Diamond Logo",
                                tint = SecondaryGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "DIAMOND",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryCyan,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Text(
                                "WALA",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryGold,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                    }

                    // Balance Display Box
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ThemeSurface)
                            .border(1.dp, SecondaryGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable { showTransactionLedger = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("balance_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Diamonds",
                            tint = SecondaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = (user?.balance ?: 0).toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextLight,
                                fontWeight = FontWeight.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "💎",
                            fontSize = 12.sp
                        )
                    }
                }
                Divider(color = ThemeSurface, thickness = 1.dp)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ThemeSurface,
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                TabItem.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            activeMiniGame = ActiveGameType.NONE
                            currentTab = tab
                        },
                        icon = {
                            val icon = when (tab) {
                                TabItem.GAMES -> Icons.Default.PlayArrow
                                TabItem.TASKS -> Icons.Default.ThumbUp
                                TabItem.REDEEM -> Icons.Default.ShoppingCart
                                TabItem.HISTORY -> Icons.Default.AccountBox
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                tint = if (selected) PrimaryCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) PrimaryCyan else TextMuted
                                )
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ThemeSurfaceVariant
                        ),
                        modifier = Modifier.testTag("tab_button_${tab.route}")
                    )
                }
            }
        },
        containerColor = ThemeBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Navigation Controller Switch
            AnimatedContent(
                targetState = Pair(currentTab, activeMiniGame),
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "ScreenTransition"
            ) { (tab, game) ->
                if (game != ActiveGameType.NONE) {
                    when (game) {
                        ActiveGameType.SPIN_WHEEL -> SpinWheelScreen(viewModel) { activeMiniGame = ActiveGameType.NONE }
                        ActiveGameType.SCRATCH_CARD -> ScratchCardScreen(viewModel) { activeMiniGame = ActiveGameType.NONE }
                        ActiveGameType.MATH_QUEST -> MathQuestScreen(viewModel) { activeMiniGame = ActiveGameType.NONE }
                        ActiveGameType.CHEST_CLICKER -> ChestChallengeScreen(viewModel) { activeMiniGame = ActiveGameType.NONE }
                        ActiveGameType.ARCADE_SIMULATOR -> ArcadeSimulatorScreen(viewModel, selectedArcadeGameName) { activeMiniGame = ActiveGameType.NONE }
                        else -> { activeMiniGame = ActiveGameType.NONE }
                    }
                } else {
                    when (tab) {
                        TabItem.GAMES -> GamesTabScreen(
                            user = user,
                            onLaunchMinigame = { activeMiniGame = it },
                            onLaunchArcade = { name ->
                                selectedArcadeGameName = name
                                activeMiniGame = ActiveGameType.ARCADE_SIMULATOR
                            }
                        )
                        TabItem.TASKS -> TasksTabScreen(
                            completedOffers = completedOffers,
                            onCompleteSurvey = { activeSurveyModal = it },
                            onWatchVideo = { activeTaskVideoModal = it }
                        )
                        TabItem.REDEEM -> RedeemTabScreen(
                            userBalance = user?.balance ?: 0,
                            onRedeemSelected = { showRedeemConfirmation = it }
                        )
                        TabItem.HISTORY -> HistoryTabScreen(
                            user = user,
                            claimedCodes = claimedCodes,
                            transactions = transactions
                        )
                    }
                }
            }

            // Transaction History Quick Ledger Sheet
            if (showTransactionLedger) {
                Dialog(onDismissRequest = { showTransactionLedger = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f),
                        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Diamond Ledger",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = PrimaryCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                IconButton(onClick = { showTransactionLedger = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLight)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            if (transactions.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Ledger",
                                        tint = TextMuted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Ledger Records Yet", color = TextMuted)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(transactions) { tx ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(ThemeSurfaceVariant, RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            if (tx.type == "EARN") GreenSuccess.copy(alpha = 0.15f)
                                                            else RedError.copy(alpha = 0.15f),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (tx.type == "EARN") Icons.Default.Add else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = if (tx.type == "EARN") GreenSuccess else RedError,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(tx.source, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextLight))
                                                    Text(
                                                        text = android.text.format.DateFormat.format("MMM dd, hh:mm a", tx.timestamp).toString(),
                                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = (if (tx.type == "EARN") "+" else "-") + tx.amount,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = if (tx.type == "EARN") GreenSuccess else RedError
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // REDEEM CONFIRMATION MODAL
            showRedeemConfirmation?.let { gcOption ->
                Dialog(onDismissRequest = { showRedeemConfirmation = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("redeem_confirm_dialog"),
                        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Confirm Redemption",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Coupon illustration
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        Brush.linearGradient(colors = listOf(CardGradientStart, CardGradientEnd)),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        gcOption.title,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = SecondaryGold,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Instant Delivery",
                                        style = MaterialTheme.typography.bodySmall.copy(color = PrimaryCyan)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "This will deduct ${gcOption.cost} Diamonds from your balance.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showRedeemConfirmation = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = TextLight)
                                }
                                Button(
                                    onClick = {
                                        viewModel.purchaseGiftCard(gcOption.title, gcOption.cost)
                                        showRedeemConfirmation = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_purchase_btn")
                                ) {
                                    Text("Redeem", color = ThemeBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SURVEY MODAL SYSTEM
            activeSurveyModal?.let { survey ->
                var currentQuestionIndex by remember { mutableStateOf(0) }
                val answers = remember { mutableStateListOf<String>() }
                val questions = survey.questions

                Dialog(onDismissRequest = { activeSurveyModal = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .testTag("survey_dialog"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = ThemeSurface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    survey.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = SecondaryGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    "Q ${currentQuestionIndex + 1}/${questions.size}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PrimaryCyan)
                                )
                            }
                            
                            // Progress bar
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = (currentQuestionIndex + 1).toFloat() / questions.size,
                                color = PrimaryCyan,
                                trackColor = ThemeSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            if (currentQuestionIndex < questions.size) {
                                val currentQuestion = questions[currentQuestionIndex]
                                Text(
                                    currentQuestion.query,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextLight
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    currentQuestion.options.forEach { option ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(ThemeSurfaceVariant, RoundedCornerShape(12.dp))
                                                .border(1.dp, PrimaryCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    answers.add(option)
                                                    if (currentQuestionIndex + 1 < questions.size) {
                                                        currentQuestionIndex++
                                                    } else {
                                                        // Finished!
                                                        viewModel.completeOfferwallTask(
                                                            offerId = survey.id,
                                                            title = survey.title,
                                                            type = "SURVEY",
                                                            reward = survey.reward,
                                                            onResult = { success ->
                                                                if (success) {
                                                                    Toast.makeText(context, "Completed! +${survey.reward}💎", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    Toast.makeText(context, "Survey rewards claimed before!", Toast.LENGTH_SHORT).show()
                                                                }
                                                                activeSurveyModal = null
                                                            }
                                                        )
                                                    }
                                                }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .border(2.dp, PrimaryCyan, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(option, color = TextLight, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // SPONSOR AD VIDEO LOADER
            activeTaskVideoModal?.let { video ->
                var secondsRemaining by remember { mutableStateOf(5) }
                LaunchedEffect(Unit) {
                    while (secondsRemaining > 0) {
                        delay(1000)
                        secondsRemaining--
                    }
                    viewModel.completeOfferwallTask(
                        offerId = video.id,
                        title = video.title,
                        type = "VIDEO",
                        reward = video.reward,
                        onResult = { success ->
                            if (success) {
                                Toast.makeText(context, "Daily Video Sponsor complete! +${video.reward}💎", Toast.LENGTH_SHORT).show()
                            }
                            activeTaskVideoModal = null
                        }
                    )
                }

                Dialog(onDismissRequest = { /* Deny cancel intermediate sponsor streaming */ }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = SecondaryGold,
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Streaming Sponsor Broadcast...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Reward credited instantly in $secondsRemaining seconds.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Fake retro screen visualizer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.Black, RoundedCornerShape(12.dp))
                                    .border(1.dp, PrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val infinityRotate = rememberInfiniteTransition()
                                val rotAngle by infinityRotate.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(4000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .rotate(rotAngle)
                                    )
                                    Text(
                                        "DIAMOND SPONSOR ADS",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// GAMES TAB SCREEN
// ------------------------------------------------------------------------
@Composable
fun GamesTabScreen(
    user: UserAccount?,
    onLaunchMinigame: (ActiveGameType) -> Unit,
    onLaunchArcade: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    var selectedArcadeCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Battle", "Racing", "Arcade", "Strategy", "Puzzle")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryCyan.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            radius = size.width / 1.5f,
                            center = Offset(size.width, 0f)
                        )
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gamer Arena Lobby",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryGold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .background(PrimaryCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "LVL ${user?.level ?: 1}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryCyan
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Play Games & Claim Free Store Redeem Codes!",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextLight,
                            fontSize = 18.sp,
                            lineHeight = 24.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1,845 Players Online • Instant code generation secure validation engine active.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }

        // Active Playable Embedded Minigames Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Super Earning Minigames",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    letterSpacing = 0.5.sp
                )
            )
            Box(
                modifier = Modifier
                    .background(PrimaryCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Double Rates", fontSize = 10.sp, color = PrimaryCyan, fontWeight = FontWeight.Bold)
            }
        }

        // Active Minigames Layout
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Spin and Scratch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinigameEntryCard(
                    title = "Diamond Spin Wheel",
                    subtitle = "Spin and Win up to 100 💎",
                    badge = "Daily Spins",
                    gradientColors = listOf(CardGradientStart, Color(0xFF331E36)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_spin_wheel"),
                    onClick = { onLaunchMinigame(ActiveGameType.SPIN_WHEEL) }
                )
                MinigameEntryCard(
                    title = "Golden Scratch Card",
                    subtitle = "Reveal 3 Premium Diamonds",
                    badge = "Infinite Luck",
                    gradientColors = listOf(Color(0xFF3D2136), CardGradientEnd),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_scratch_card"),
                    onClick = { onLaunchMinigame(ActiveGameType.SCRATCH_CARD) }
                )
            }
            // Math Quest and Chest Tap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinigameEntryCard(
                    title = "Gamer Math Quest",
                    subtitle = "Solve simple math in 10s",
                    badge = "+15 💎 Each",
                    gradientColors = listOf(Color(0xFF142C33), CardGradientStart),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_math_quest"),
                    onClick = { onLaunchMinigame(ActiveGameType.MATH_QUEST) }
                )
                MinigameEntryCard(
                    title = "Treasure Chest",
                    subtitle = "Extreme click challenge!",
                    badge = "Blitz Reward",
                    gradientColors = listOf(CardGradientEnd, Color(0xFF101B3A)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_chest_clicker"),
                    onClick = { onLaunchMinigame(ActiveGameType.CHEST_CLICKER) }
                )
            }
        }

        // CATALOGUE OF 200+ EXCITE CLOUD GAMES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "200+ Exciting Cloud Arcade",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                )
                Text(
                    "Play classic arcade hits to earn daily bonus chest",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        // Categories selector row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val active = selectedArcadeCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) PrimaryCyan else ThemeSurface)
                        .clickable { selectedArcadeCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        cat,
                        color = if (active) ThemeBackground else TextLight,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Custom Arcade Grid
        val allArcadeGames = listOf(
            ArcadeGameMock("Solitaire Grand Master", "Puzzle", "4.8", "25K Playing"),
            ArcadeGameMock("Flappy Diamond", "Arcade", "4.5", "10K Playing"),
            ArcadeGameMock("Bendy Neo Racer", "Racing", "4.2", "18K Playing"),
            ArcadeGameMock("Free Fire Quiz Duel", "Battle", "4.9", "45K Playing"),
            ArcadeGameMock("Ludo Empire King", "Strategy", "4.6", "30K Playing"),
            ArcadeGameMock("Fruit Blade Slicer", "Arcade", "4.4", "15K Playing"),
            ArcadeGameMock("Tomb Cyber Runner", "Adventure", "4.7", "22K Playing"),
            ArcadeGameMock("Sudoku Token Crack", "Puzzle", "4.3", "8K Playing")
        )
        val filteredArcadeGames = if (selectedArcadeCategory == "All") allArcadeGames else allArcadeGames.filter { it.category == selectedArcadeCategory }

        if (filteredArcadeGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("More awesome games loading shortly in this category!", color = TextMuted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredArcadeGames.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { gameItem ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ThemeSurface)
                                    .border(1.dp, ThemeSurfaceVariant, RoundedCornerShape(14.dp))
                                    .clickable { onLaunchArcade(gameItem.name) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        ThemeSurfaceVariant,
                                                        Color(0xFF231E3D)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (gameItem.category) {
                                                "Puzzle" -> Icons.Default.Info
                                                "Racing" -> Icons.Default.PlayArrow
                                                "Battle" -> Icons.Default.Star
                                                "Strategy" -> Icons.Default.Build
                                                else -> Icons.Default.PlayArrow
                                            },
                                            contentDescription = null,
                                            tint = PrimaryCyan.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = gameItem.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(gameItem.category, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted), fontSize = 10.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(gameItem.rating, style = MaterialTheme.typography.bodySmall.copy(color = TextLight), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MinigameEntryCard(
    title: String,
    subtitle: String,
    badge: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = gradientColors))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(PrimaryCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badge, fontSize = 9.sp, color = PrimaryCyan, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(color = TextLight, fontWeight = FontWeight.ExtraBold))
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// Data Classes for UI models
data class ArcadeGameMock(val name: String, val category: String, val rating: String, val activePlayers: String)

// ------------------------------------------------------------------------
// SPIN WHEEL MODULE SCREEN
// ------------------------------------------------------------------------
@Composable
fun SpinWheelScreen(viewModel: DiamondWalaViewModel, onGoBack: () -> Unit) {
    val rotationStateAngle by animateFloatAsState(
        targetValue = viewModel.targetRotationAngle,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        label = "WheelRotation"
    )

    val spinResult = viewModel.spinResultReward
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoBack, modifier = Modifier.testTag("back_button_from_game")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
            }
            Text("Diamond Spin Wheel", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Test your galactic luck! Tap SPIN to rotate the cyber gold engine. Points are accumulated automatically.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(30.dp))

        // Canvas Spin Wheel Design
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(260.dp)
                    .rotate(rotationStateAngle)
            ) {
                val canvasSize = size
                val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                val radius = canvasSize.width / 2

                // Segment slices colors - Cyber contrast
                val sliceColors = listOf(
                    Color(0xFF2C1A3D), Color(0xFF132B3C),
                    Color(0xFF2C1A3D), Color(0xFF132B3C),
                    Color(0xFF2C1A3D), Color(0xFF132B3C),
                    Color(0xFF2C1A3D), Color(0xFF132B3C)
                )
                val segmentLabels = listOf("TRY AGAIN", "+10 💎", "+25 💎", "+5 💎", "+50 💎", "+100 💎", "+15 💎", "+30 💎")

                for (i in 0 until 8) {
                    val startAngle = i * 45f
                    drawArc(
                        color = sliceColors[i],
                        startAngle = startAngle,
                        sweepAngle = 45f,
                        useCenter = true,
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                    
                    // Outlines for polish
                    drawArc(
                        color = PrimaryCyan.copy(alpha = 0.3f),
                        startAngle = startAngle,
                        sweepAngle = 45f,
                        useCenter = true,
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Embedded Texts in segments
                    val angleRad = (startAngle + 22.5f) * (PI / 180f)
                    val textDistance = radius * 0.65f
                    val labelX = center.x + textDistance * cos(angleRad).toFloat()
                    val labelY = center.y + textDistance * sin(angleRad).toFloat()

                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.translate(labelX, labelY)
                    drawContext.canvas.nativeCanvas.rotate(startAngle + 22.5f + 90f)

                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 10.dp.toPx()
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        segmentLabels[i],
                        0f, 0f,
                        paint
                    )
                    drawContext.canvas.nativeCanvas.restore()
                }

                // Cyber outer golden ring
                drawCircle(
                    color = SecondaryGold,
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // Pointer center pin
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SecondaryGold, CircleShape)
                    .border(2.dp, PrimaryCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ThemeBackground,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(-90f)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Play Button or Loading Status
        Button(
            onClick = { viewModel.spinWheel() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isSpinning) TextMuted else SecondaryGold
            ),
            enabled = !viewModel.isSpinning,
            modifier = Modifier
                .width(180.dp)
                .height(48.dp)
                .testTag("spin_action_button")
        ) {
            Text(
                if (viewModel.isSpinning) "Spinning..." else "SPIN NOW",
                color = ThemeBackground,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Attempts remaining: ${viewModel.spinsRemaining} times",
            color = TextLight,
            style = MaterialTheme.typography.bodyMedium
        )

        // Show Spin Finish Result Banner Dialog
        if (spinResult != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SPIN RESULTS", style = MaterialTheme.typography.bodySmall.copy(color = SecondaryGold))
                        Text(
                            text = if (spinResult > 0) "Captured +$spinResult Diamonds!" else "Unlucky, try again!",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextLight, fontWeight = FontWeight.Bold)
                        )
                    }
                    Button(
                        onClick = { viewModel.resetSpinResult() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK", color = ThemeBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// SCRATCH CARD GAMES MODULE
// ------------------------------------------------------------------------
@Composable
fun ScratchCardScreen(viewModel: DiamondWalaViewModel, onGoBack: () -> Unit) {
    val context = LocalContext.current
    val scratchedAmt = viewModel.scratchCardScratchedAmount
    val revealed = viewModel.isScratchCardRevealed
    val reward = viewModel.scratchCardReward
    val tiles = viewModel.scratchGridValues

    LaunchedEffect(Unit) {
        viewModel.prepareNewScratchCard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoBack, modifier = Modifier.testTag("back_button_from_scratch")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
            }
            Text("Golden Scratch Card", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Rub the metallic gold surface to clean the coating and reveal hidden tokens. Settle at 100% to credit instantly.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Scratch Container Area
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ThemeSurface)
                .border(2.dp, ThemeSurfaceVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Background Layer: The Rewards under the coating
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "YOUR SCRATCH REWARD",
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryGold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tiles.forEach { tile ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(ThemeSurfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tile == "DIAMOND") "💎" else if (tile == "COIN") "💰" else "❌",
                                fontSize = 22.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (reward > 0) "+$reward Diamonds!" else "Try your luck next time!",
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextLight, fontWeight = FontWeight.ExtraBold)
                )
            }

            // Foreground Layer: Gold Scratch Coating
            if (!revealed) {
                // Interactive scratch card simulated by dragging/tapping
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(SecondaryGold, Color(0xFFD4AF37))
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.performScratch(2)
                            }
                        }
                        .clickable { viewModel.performScratch(12) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = ThemeBackground, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SCRATCH GENTLY HERE",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = ThemeBackground,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Cleaned: $scratchedAmt%",
                            color = ThemeBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (revealed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "CONGRATULATIONS!",
                        fontWeight = FontWeight.Bold,
                        color = GreenSuccess
                    )
                    Text(
                        "Granted +$reward diamonds successfully logged in your transactions history.",
                        color = TextLight,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                viewModel.prepareNewScratchCard()
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            modifier = Modifier.testTag("next_scratch_card_btn")
        ) {
            Text("Next Scratch Card", color = ThemeBackground, fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------------
// MATH QUEST MINI GAME
// ------------------------------------------------------------------------
@Composable
fun MathQuestScreen(viewModel: DiamondWalaViewModel, onGoBack: () -> Unit) {
    val context = LocalContext.current
    
    val valA = viewModel.mathQuizValueA
    val valB = viewModel.mathQuizValueB
    val op = viewModel.mathQuizOperation
    val choices = viewModel.mathQuizChoices
    val answered = viewModel.isMathQuizAnswered
    val wasCorrect = viewModel.wasMathQuizCorrect

    LaunchedEffect(Unit) {
        viewModel.generateNewMathQuiz()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoBack, modifier = Modifier.testTag("back_button_from_math")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
            }
            Text("Gamer Math Quest", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Enhance your calculation index! Submit the correct answer. Speed checks earn you diamonds.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Equation Card Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.3.dp, PrimaryCyan.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("SOLVE EQUATION", style = MaterialTheme.typography.bodySmall.copy(color = SecondaryGold, letterSpacing = 1.sp))
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$valA $op $valB = ?",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextLight,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Choices Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { choice ->
                        Button(
                            onClick = { viewModel.submitMathQuizAnswer(choice) },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ThemeSurfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text(
                                choice.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (answered) {
            val isSuccess = wasCorrect == true
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess) GreenSuccess.copy(alpha = 0.15f) else RedError.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, if (isSuccess) GreenSuccess else RedError),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSuccess) "EXCELLENT WORK!" else "WRONG ANSWER",
                        color = if (isSuccess) GreenSuccess else RedError,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSuccess) "Correct! +${viewModel.mathQuizReward} diamonds deposited in your vault."
                               else "The correct calculation was ${viewModel.mathQuizCorrectAnswer}. Give it another shot!",
                        color = TextLight,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.generateNewMathQuiz() },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            modifier = Modifier.testTag("next_math_quest_btn")
        ) {
            Text("Next Calculation", color = ThemeBackground, fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------------
// TREASURE CHEST CLICKER CHALLENGE
// ------------------------------------------------------------------------
@Composable
fun ChestChallengeScreen(viewModel: DiamondWalaViewModel, onGoBack: () -> Unit) {
    val progress = viewModel.chestClickProgress
    val scale = animateFloatAsState(targetValue = 1f + (progress * 0.15f), animationSpec = spring())
    val clicksRemaining = viewModel.chestClicksLeft
    val seconds = viewModel.secondsLeftChest
    val reward = viewModel.chestRewardGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
            }
            Text("Treasure Chest Challenge", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Tap speed drill! Break open the solid diamond chest under 15 seconds. High speed unlocks the hoard.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Timer Gauge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ThemeSurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = RedError, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("00:${if (seconds < 10) "0$seconds" else seconds}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextLight))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Chest Illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .rotate(if (clicksRemaining in 1..39) (Random.nextInt(-4, 4)).toFloat() else 0f)
                .clickable { viewModel.clickChest() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = size
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(PrimaryCyan.copy(alpha = 0.12f), Color.Transparent)),
                    radius = size.width * 0.45f
                )
            }
            
            // Draw a neat vector-styled Treasure Box on Canvas
            Canvas(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
            ) {
                val w = size.width
                val h = size.height
                // Draw lid
                drawRect(
                    color = SecondaryGold,
                    topLeft = Offset(w * 0.2f, h * 0.25f),
                    size = Size(w * 0.6f, h * 0.2f)
                )
                // Draw lower compartment
                drawRect(
                    color = Color(0xFFC59B27),
                    topLeft = Offset(w * 0.2f, h * 0.48f),
                    size = Size(w * 0.6f, h * 0.35f)
                )
                // Draw cyber neon locks
                drawCircle(
                    color = PrimaryCyan,
                    radius = w * 0.08f,
                    center = Offset(w * 0.5f, h * 0.48f)
                )
                // Iron bands
                drawRect(
                    color = ThemeBackground,
                    topLeft = Offset(w * 0.3f, h * 0.25f),
                    size = Size(w * 0.08f, h * 0.58f)
                )
                drawRect(
                    color = ThemeBackground,
                    topLeft = Offset(w * 0.62f, h * 0.25f),
                    size = Size(w * 0.08f, h * 0.58f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Taps Left: $clicksRemaining times",
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryCyan,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            color = PrimaryCyan,
            trackColor = ThemeSurface,
            modifier = Modifier
                .width(200.dp)
                .height(8.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (reward != null) {
            if (reward > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CHEST CRACKED!", fontWeight = FontWeight.Bold, color = GreenSuccess)
                        Text("Nice mashing rate! Vault deposit of +$reward diamonds processed.", fontSize = 12.sp, color = TextLight, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CHALLENGE EXPIRED", fontWeight = FontWeight.Bold, color = RedError)
                        Text("You timed out! Retain speed and click faster to crack the locker.", fontSize = 12.sp, color = TextLight, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.startChestChallenge() },
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold),
            modifier = Modifier.testTag("start_chest_btn")
        ) {
            Text("Launch Tap Challenge", color = ThemeBackground, fontWeight = FontWeight.Bold)
        }
    }
}

// Helper canvas extension for scaling
private fun Modifier.scale(scale: Float): Modifier = this.drawBehind {
    // Standard drawing operations remain unaffected by simple composition scale attributes
}

// ------------------------------------------------------------------------
// ARCADE GAMES LOADER SIMULATOR
// ------------------------------------------------------------------------
@Composable
fun ArcadeSimulatorScreen(viewModel: DiamondWalaViewModel, gameName: String, onGoBack: () -> Unit) {
    val context = LocalContext.current
    var loadingPercent by remember { mutableStateOf(0) }
    var activeGameplay by remember { mutableStateOf(false) }
    var gameScore by remember { mutableStateOf(0) }
    
    // Multi items capture targets
    var diamondOffsetA by remember { mutableStateOf(Offset(50f, 100f)) }
    var diamondOffsetB by remember { mutableStateOf(Offset(200f, 250f)) }

    LaunchedEffect(Unit) {
        // Loading animation
        while (loadingPercent < 100) {
            delay((40..90).random().toLong())
            loadingPercent += 4
        }
        activeGameplay = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
            }
            Text(gameName, style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        }

        if (!activeGameplay) {
            // Simulated Arcade Loading Panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = loadingPercent.toFloat() / 100f,
                    color = PrimaryCyan,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "CONSTRUCTING SECURE SANDBOX...",
                    style = TextStyle(fontFamily = FontFamily.Monospace, color = SecondaryGold)
                )
                Text(
                    "Establishing game hooks $loadingPercent%...",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        } else {
            // Active Gameplay Window
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Capture Score: $gameScore", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    Text("Secure Cloud Stream", color = GreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive canvas play container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.5.dp, ThemeSurfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    // Flying Diamond A
                    Box(
                        modifier = Modifier
                            .offset(diamondOffsetA.x.dp, diamondOffsetA.y.dp)
                            .size(44.dp)
                            .background(PrimaryCyan.copy(alpha = 0.25f), CircleShape)
                            .clickable {
                                gameScore++
                                // Relocate
                                diamondOffsetA = Offset(
                                    (10..220)
                                        .random()
                                        .toFloat(),
                                    (10..300)
                                        .random()
                                        .toFloat()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💎", fontSize = 20.sp)
                    }

                    // Flying Diamond B
                    Box(
                        modifier = Modifier
                            .offset(diamondOffsetB.x.dp, diamondOffsetB.y.dp)
                            .size(44.dp)
                            .background(SecondaryGold.copy(alpha = 0.25f), CircleShape)
                            .clickable {
                                gameScore += 2
                                diamondOffsetB = Offset(
                                    (10..220)
                                        .random()
                                        .toFloat(),
                                    (10..300)
                                        .random()
                                        .toFloat()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💰", fontSize = 20.sp)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(ThemeSurface.copy(alpha = 0.8f))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TAP REFLEX DIAMONDS AS THEY APPEAR!", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Target: Reach 8 points to secure arcade bonus chest.", color = TextMuted, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (gameScore >= 8) {
                    Button(
                        onClick = {
                            viewModel.earnTokens(12, "Arcade: $gameName")
                            Toast.makeText(context, "Arcade Bonus +12💎 Claimed!", Toast.LENGTH_SHORT).show()
                            onGoBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("claim_arcade_reward_btn")
                    ) {
                        Text("CLAIM GAME REWARD (+12 💎)", color = ThemeBackground, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onGoBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Quit Session", color = TextLight)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// TASKS & SURVEYS TAB SCREEN (OFFERWALL)
// ------------------------------------------------------------------------
@Composable
fun TasksTabScreen(
    completedOffers: List<CompletedOffer>,
    onCompleteSurvey: (SurveyMock) -> Unit,
    onWatchVideo: (TaskMock) -> Unit
) {
    val scrollState = rememberScrollState()

    // Mock Static Offer datasets
    val mockSurveys = listOf(
        SurveyMock(
            id = "srv_setup",
            title = "Gamers Battle Survey",
            reward = 120,
            duration = "3 Mins",
            questions = listOf(
                SurveyQuestion("Which competitive game is your main title?", listOf("Free Fire", "PUBG Mobile", "Call of Duty Mobile", "Other")),
                SurveyQuestion("What device class do you perform gameplay on?", listOf("Premium Flagship", "Mid-tier Budget", "Casual Tablet", "PC Emulator")),
                SurveyQuestion("Which reward category attracts you the most?", listOf("Google Play Gift Codes", "Free Fire Direct Diamonds", "Steam Codes", "App Store Voucher"))
            )
        ),
        SurveyMock(
            id = "srv_habits",
            title = "Social Mobile Preferences",
            reward = 180,
            duration = "4 Mins",
            questions = listOf(
                SurveyQuestion("Do you watch game streaming streams?", listOf("Yes, daily", "Sometimes on weekends", "Rarely", "Never")),
                SurveyQuestion("Preferred broadcast platform?", listOf("YouTube Gaming", "Twitch", "Discord Channel", "Facebook Live")),
                SurveyQuestion("Have you ever purchased digital game coins?", listOf("Yes, multiple times", "Only once", "Never, purely free player"))
            )
        )
    )

    val mockTasks = listOf(
        TaskMock("tsk_raid", "Install Raid Legend Saga", "Download and defeat boss level 5", 480, "Easy Setup"),
        TaskMock("tsk_crypto", "Complete Crypto Trading Registration", "Standard signup verification", 350, "Registration"),
        TaskMock("tsk_solitaire", "Download Grand Solitaire Castle", "Open & tap 10 items", 150, "Quick Play")
    )

    val mockVideos = listOf(
        TaskMock("vid_sponsor1", "Watch Tech Sponsor Ad", "Review short sponsor clip", 30, "+30 💎"),
        TaskMock("vid_sponsor2", "Watch Game Trailer Review", "Review game preview clips", 25, "+25 💎")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Offerwall Header Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            border = BorderStroke(1.dp, ThemeSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Official Offerwall Tasks", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Answer gamer preference surveys and complete sponsor tasks to unlock huge volumes of diamonds.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }

        // SURVEYS PART
        Text("High Value Surveys", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mockSurveys.forEach { survey ->
                val alreadyDone = completedOffers.any { it.offerId == survey.id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeSurface)
                        .border(
                            1.dp,
                            if (alreadyDone) ThemeSurfaceVariant else PrimaryCyan.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !alreadyDone) { onCompleteSurvey(survey) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(survey.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextLight))
                            if (alreadyDone) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(GreenSuccess.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("CLAIMED", fontSize = 8.sp, color = GreenSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Duration: ${survey.duration} • Instantly Credits", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (alreadyDone) ThemeSurfaceVariant else SecondaryGold.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+${survey.reward} 💎",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alreadyDone) TextMuted else SecondaryGold
                            )
                        )
                    }
                }
            }
        }

        // VIDEO SPONSORS PART
        Text("Daily Sponsor Video Drops", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mockVideos.forEach { video ->
                val alreadyDone = completedOffers.any { it.offerId == video.id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeSurface)
                        .border(1.dp, ThemeSurfaceVariant, RoundedCornerShape(14.dp))
                        .clickable(enabled = !alreadyDone) { onWatchVideo(video) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryCyan)
                        }
                        Column {
                            Text(video.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextLight))
                            Text(video.description, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(ThemeSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (alreadyDone) "DONE" else "+${video.reward} 💎",
                            fontSize = 11.sp,
                            color = if (alreadyDone) TextMuted else PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // REDEEM VERIFIED PARTNER OFFERWALLS
        Text("Partner Tasks", style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mockTasks.forEach { task ->
                val alreadyDone = completedOffers.any { it.offerId == task.id }
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeSurface)
                        .border(1.dp, ThemeSurfaceVariant, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextLight))
                        Text(task.description, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "Partner App Redirect: Simulated Task Launch!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("PLAY", color = ThemeBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

data class SurveyMock(val id: String, val title: String, val reward: Int, val duration: String, val questions: List<SurveyQuestion>)
data class SurveyQuestion(val query: String, val options: List<String>)
data class TaskMock(val id: String, val title: String, val description: String, val reward: Int, val categoryTag: String)

// ------------------------------------------------------------------------
// REDEEM CODES STORE TAB SCREEN
// ------------------------------------------------------------------------
@Composable
fun RedeemTabScreen(
    userBalance: Int,
    onRedeemSelected: (GiftCardOption) -> Unit
) {
    val scrollState = rememberScrollState()

    val redeemOptions = listOf(
        GiftCardOption("Google Play Gift Card $1", 1000, "Google Play"),
        GiftCardOption("Google Play Gift Card $2", 1800, "Google Play"),
        GiftCardOption("Google Play Gift Card $5", 4200, "Google Play"),
        GiftCardOption("Google Play Gift Card $10", 8000, "Google Play"),
        GiftCardOption("Steam Wallet Code $5", 4500, "Steam Wallet"),
        GiftCardOption("Steam Wallet Code $10", 8500, "Steam Wallet"),
        GiftCardOption("App Store Gift Code $5", 5000, "iOS Voucher"),
        GiftCardOption("Free Fire: 220 Diamonds", 1500, "Free Fire Game"),
        GiftCardOption("Free Fire: 530 Diamonds", 3200, "Free Fire Game")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vault Header
        Card(
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            border = BorderStroke(1.dp, ThemeSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Store Reward Centre",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextLight, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Redeem your accumulated diamonds for instant, functional vouchers and game gift codes directly delivered into your vault.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        Text(
            "Available Gift Cards",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
        )

        // Store Layout
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            redeemOptions.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { option ->
                        val canAfford = userBalance >= option.cost
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ThemeSurface)
                                .border(
                                    1.2.dp,
                                    if (canAfford) SecondaryGold.copy(alpha = 0.5f) else ThemeSurfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onRedeemSelected(option) }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    CardGradientStart,
                                                    if (canAfford) CardGradientEnd else Color(0xFF26262B)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            option.category,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = PrimaryCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = if (canAfford) SecondaryGold else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Text(
                                    option.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${option.cost} 💎",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = SecondaryGold
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (canAfford) PrimaryCyan.copy(alpha = 0.15f)
                                                else ThemeSurfaceVariant
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (canAfford) "CLAIM" else "LOCK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (canAfford) PrimaryCyan else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

data class GiftCardOption(val title: String, val cost: Int, val category: String)

// ------------------------------------------------------------------------
// MY WALLET & HISTORY TAB SCREEN
// ------------------------------------------------------------------------
@Composable
fun HistoryTabScreen(
    user: UserAccount?,
    claimedCodes: List<ClaimedCode>,
    transactions: List<TokenTransaction>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glowing Profile Avatar Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ThemeSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vector Canvas User Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(PrimaryCyan.copy(alpha = 0.1f), CircleShape)
                        .border(1.5.dp, PrimaryCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "Avatar",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user?.username ?: "Golden Gamer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Member Level Rank: ${user?.level ?: 1}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Level progression representation
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val progressPercent = ((user?.totalEarned ?: 0) % 1000).toFloat() / 1000f
                        LinearProgressIndicator(
                            progress = progressPercent,
                            color = PrimaryCyan,
                            trackColor = ThemeSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("XP to Level Up", fontSize = 9.sp, color = TextMuted)
                            Text("${((user?.totalEarned ?: 0) % 1000)}/1000", fontSize = 9.sp, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // REDEEMED CODE VAULT
        Text(
            "My Redeemed Codes Vault",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
        )

        if (claimedCodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillModifierCompactWidth()
                    .height(130.dp)
                    .background(ThemeSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, ThemeSurfaceVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Redeemed Vouchers Yet", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                claimedCodes.forEach { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ThemeSurface)
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                code.itemTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextLight
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = code.redeemCode,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = SecondaryGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Acquired " + android.text.format.DateFormat.format("yyyy-MM-dd", code.timestamp).toString(),
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                fontSize = 9.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code.redeemCode))
                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Copy code",
                                tint = PrimaryCyan
                            )
                        }
                    }
                }
            }
        }

        // MANDATORY DISCLAIMER FOR METADATA INTEGRITY
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant),
            border = BorderStroke(1.5.dp, SecondaryGold.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Disclaimer Information",
                        tint = SecondaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Disclaimer Notice",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SecondaryGold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Diamond Wala respects copyright, and all rights belong to their respective owners. Our app is not linked to, sponsored by, or endorsed by any company names, brand labels or console platforms mentioned.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Custom responsive helper modifiers
fun Modifier.fillModifierCompactWidth(): Modifier = this.fillMaxWidth()
