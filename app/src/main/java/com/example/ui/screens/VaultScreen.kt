package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.TrackEntity
import com.example.data.local.VaultItemEntity
import com.example.util.TimeUtils

enum class VaultLockType {
    PIN_6_DIGIT,
    PATTERN
}

@Composable
fun VaultScreen(
    vaultItems: List<VaultItemEntity>,
    availableAudioTracks: List<TrackEntity> = emptyList(),
    availableVideoTracks: List<TrackEntity> = emptyList(),
    onRestoreItem: (VaultItemEntity) -> Unit,
    onDeleteItemPermanently: (VaultItemEntity) -> Unit,
    onAddMediaToVault: (uri: String, title: String, type: String) -> Unit,
    onHideAppTrackToVault: (TrackEntity) -> Unit = {},
    onPlayVaultTrack: (TrackEntity) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waveplay_vault", Context.MODE_PRIVATE) }

    val savedLockTypeStr = prefs.getString("lock_type", null)
    val savedSecret = prefs.getString("secret", null)

    var isSetupMode by remember { mutableStateOf(savedSecret == null) }
    var isUnlocked by remember { mutableStateOf(false) }

    // Predefined Security Questions (Extensive options + Custom)
    val securityQuestions = remember {
        listOf(
            "What is your childhood nickname?",
            "Who is your favorite singer or music artist?",
            "What city were you born in?",
            "What was the name of your first school?",
            "What is your favorite food or dish?",
            "What was the name of your first pet?",
            "What is your mother's maiden name?",
            "Custom Question..."
        )
    }

    // Setup state
    var selectedSetupType by remember { mutableStateOf(VaultLockType.PIN_6_DIGIT) }
    var setupInput by remember { mutableStateOf("") }
    var setupConfirmInput by remember { mutableStateOf("") }
    var setupStep by remember { mutableIntStateOf(1) } // 1: Enter, 2: Confirm, 3: Security Question
    var setupSecurityQuestion by remember { mutableStateOf(securityQuestions[0]) }
    var customQuestionInput by remember { mutableStateOf("") }
    var setupSecurityAnswer by remember { mutableStateOf("") }
    var showQuestionDropdown by remember { mutableStateOf(false) }

    // Unlock state
    var unlockInput by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf(false) }

    // Forgot Password state
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotAnswerInput by remember { mutableStateOf("") }
    var forgotAnswerError by remember { mutableStateOf(false) }

    // Vault dashboard state
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Audios, 1: Videos, 2: Photos
    var showHideMediaDialog by remember { mutableStateOf(false) }
    var hideMediaTab by remember { mutableIntStateOf(0) } // 0: Audios in App, 1: Videos in App, 2: Device Files
    var hideMediaQuery by remember { mutableStateOf("") }
    var showVaultSecuritySettings by remember { mutableStateOf(false) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    // Media pickers for external device files
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onAddMediaToVault(uri.toString(), "Private Audio", "AUDIO")
            Toast.makeText(context, "Audio hidden in Safe Vault (purged from public storage)", Toast.LENGTH_SHORT).show()
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onAddMediaToVault(uri.toString(), "Private Video", "VIDEO")
            Toast.makeText(context, "Video hidden in Safe Vault (purged from public storage)", Toast.LENGTH_SHORT).show()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onAddMediaToVault(uri.toString(), "Private Photo", "PHOTO")
            Toast.makeText(context, "Photo hidden in Safe Vault (purged from public storage)", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("safe_vault_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isSetupMode) {
            // STEP A: Setup Lock (6-Digit PIN or Pattern) + Security Question
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (setupStep) {
                        1 -> "Setup Safe Vault Lock"
                        2 -> "Confirm Vault Lock"
                        else -> "Set Security Recovery Question"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (setupStep) {
                        1 -> "Step 1 of 3: Choose 6-Digit PIN or Pattern"
                        2 -> "Step 2 of 3: Re-enter to confirm your lock"
                        else -> "Step 3 of 3: Used to reset password if you ever forget it"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (setupStep < 3) {
                    // Choose Lock Type: PIN vs Pattern
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = selectedSetupType == VaultLockType.PIN_6_DIGIT,
                            onClick = {
                                selectedSetupType = VaultLockType.PIN_6_DIGIT
                                setupInput = ""
                                setupConfirmInput = ""
                                setupStep = 1
                            },
                            label = { Text("6-Digit PIN") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = selectedSetupType == VaultLockType.PATTERN,
                            onClick = {
                                selectedSetupType = VaultLockType.PATTERN
                                setupInput = ""
                                setupConfirmInput = ""
                                setupStep = 1
                            },
                            label = { Text("Pattern Lock") },
                            leadingIcon = { Icon(Icons.Default.Pattern, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedSetupType == VaultLockType.PIN_6_DIGIT) {
                        Text(
                            text = if (setupStep == 1) "Enter a 6-digit PIN" else "Confirm your 6-digit PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val currentPin = if (setupStep == 1) setupInput else setupConfirmInput
                        PinDotsRow(filledCount = currentPin.length, total = 6)

                        Spacer(modifier = Modifier.height(24.dp))

                        NumericKeypad(
                            onDigit = { d ->
                                if (setupStep == 1) {
                                    if (setupInput.length < 6) setupInput += d
                                    if (setupInput.length == 6) setupStep = 2
                                } else {
                                    if (setupConfirmInput.length < 6) setupConfirmInput += d
                                    if (setupConfirmInput.length == 6) {
                                        if (setupConfirmInput == setupInput) {
                                            setupStep = 3 // Advance to Security Question
                                        } else {
                                            Toast.makeText(context, "PINs did not match, try again!", Toast.LENGTH_SHORT).show()
                                            setupConfirmInput = ""
                                            setupStep = 1
                                            setupInput = ""
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                if (setupStep == 1) {
                                    if (setupInput.isNotEmpty()) setupInput = setupInput.dropLast(1)
                                } else {
                                    if (setupConfirmInput.isNotEmpty()) setupConfirmInput = setupConfirmInput.dropLast(1)
                                }
                            }
                        )
                    } else {
                        // Pattern Lock Setup
                        Text(
                            text = if (setupStep == 1) "Connect at least 4 dots" else "Confirm pattern",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val patternValue = if (setupStep == 1) setupInput else setupConfirmInput
                        Text(
                            text = "Sequence: $patternValue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Pattern3x3Grid(
                            selectedSequence = patternValue,
                            onDotClicked = { dot ->
                                if (setupStep == 1) {
                                    if (!setupInput.contains(dot.toString())) setupInput += dot
                                } else {
                                    if (!setupConfirmInput.contains(dot.toString())) setupConfirmInput += dot
                                }
                            },
                            onClear = {
                                if (setupStep == 1) setupInput = "" else setupConfirmInput = ""
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (setupStep == 1) {
                                    if (setupInput.length >= 4) {
                                        setupStep = 2
                                    } else {
                                        Toast.makeText(context, "Connect at least 4 dots", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (setupConfirmInput == setupInput) {
                                        setupStep = 3 // Advance to Security Question
                                    } else {
                                        Toast.makeText(context, "Patterns did not match, try again!", Toast.LENGTH_SHORT).show()
                                        setupConfirmInput = ""
                                        setupStep = 1
                                        setupInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (setupStep == 1) "Continue" else "Confirm Pattern")
                        }
                    }
                } else {
                    // STEP 3: Security Question Setup
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Select a Question:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { showQuestionDropdown = true }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = setupSecurityQuestion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = showQuestionDropdown,
                                    onDismissRequest = { showQuestionDropdown = false }
                                ) {
                                    securityQuestions.forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q) },
                                            onClick = {
                                                setupSecurityQuestion = q
                                                showQuestionDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (setupSecurityQuestion == "Custom Question...") {
                                Text(
                                    text = "Your Custom Question:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = customQuestionInput,
                                    onValueChange = { customQuestionInput = it },
                                    placeholder = { Text("Type your secret question here") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = "Secret Answer:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = setupSecurityAnswer,
                                onValueChange = { setupSecurityAnswer = it },
                                placeholder = { Text("Enter your recovery answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    val finalQuestion = if (setupSecurityQuestion == "Custom Question..." && customQuestionInput.trim().isNotBlank()) {
                                        customQuestionInput.trim()
                                    } else {
                                        setupSecurityQuestion
                                    }

                                    if (setupSecurityAnswer.trim().isNotBlank()) {
                                        prefs.edit()
                                            .putString("lock_type", selectedSetupType.name)
                                            .putString("secret", setupInput)
                                            .putString("security_question", finalQuestion)
                                            .putString("security_answer", setupSecurityAnswer.trim().lowercase())
                                            .apply()
                                        isSetupMode = false
                                        isUnlocked = true
                                        Toast.makeText(context, "Safe Vault configured securely!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter an answer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = setupSecurityAnswer.trim().isNotBlank() && (setupSecurityQuestion != "Custom Question..." || customQuestionInput.trim().isNotBlank()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete Vault Setup")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TextButton(onClick = onBack) {
                    Text("Cancel")
                }
            }
        } else if (!isUnlocked) {
            // STEP B: Unlock Vault
            val lockType = if (savedLockTypeStr == VaultLockType.PATTERN.name) VaultLockType.PATTERN else VaultLockType.PIN_6_DIGIT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (unlockError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Safe Vault Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (unlockError) "Incorrect ${if (lockType == VaultLockType.PATTERN) "pattern" else "PIN"}! Try again." else "Enter your ${if (lockType == VaultLockType.PATTERN) "pattern" else "6-digit PIN"} to access hidden files",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unlockError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (lockType == VaultLockType.PIN_6_DIGIT) {
                    PinDotsRow(filledCount = unlockInput.length, total = 6, isError = unlockError)
                    Spacer(modifier = Modifier.height(24.dp))
                    NumericKeypad(
                        onDigit = { d ->
                            if (unlockInput.length < 6) {
                                unlockError = false
                                unlockInput += d
                                if (unlockInput.length == 6) {
                                    if (unlockInput == savedSecret) {
                                        isUnlocked = true
                                    } else {
                                        unlockError = true
                                        unlockInput = ""
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (unlockInput.isNotEmpty()) {
                                unlockInput = unlockInput.dropLast(1)
                                unlockError = false
                            }
                        }
                    )
                } else {
                    Pattern3x3Grid(
                        selectedSequence = unlockInput,
                        onDotClicked = { dot ->
                            if (!unlockInput.contains(dot.toString())) {
                                unlockInput += dot
                                unlockError = false
                            }
                        },
                        onClear = { unlockInput = "" }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (unlockInput == savedSecret) {
                                isUnlocked = true
                            } else {
                                unlockError = true
                                unlockInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Forgot Password Button
                TextButton(
                    onClick = {
                        forgotAnswerInput = ""
                        forgotAnswerError = false
                        showForgotDialog = true
                    }
                ) {
                    Text(
                        text = "Forgot PIN / Pattern?",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Forgot Password Dialog
            if (showForgotDialog) {
                val savedQuestion = prefs.getString("security_question", "What is your childhood nickname?") ?: "What is your childhood nickname?"
                AlertDialog(
                    onDismissRequest = { showForgotDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Vault Password", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Answer your security question to reset your password:")
                            Text(savedQuestion, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = forgotAnswerInput,
                                onValueChange = {
                                    forgotAnswerInput = it
                                    forgotAnswerError = false
                                },
                                placeholder = { Text("Your answer") },
                                singleLine = true,
                                isError = forgotAnswerError,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (forgotAnswerError) {
                                Text("Incorrect answer! Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val savedAnswer = prefs.getString("security_answer", "") ?: ""
                            val isMatch = (savedAnswer.isNotBlank() && forgotAnswerInput.trim().equals(savedAnswer.trim(), ignoreCase = true)) ||
                                    (savedAnswer.isBlank() && forgotAnswerInput.trim().isNotEmpty())
                            if (isMatch) {
                                showForgotDialog = false
                                prefs.edit().remove("secret").remove("lock_type").apply()
                                isSetupMode = true
                                setupStep = 1
                                setupInput = ""
                                setupConfirmInput = ""
                                setupSecurityAnswer = ""
                                Toast.makeText(context, "Identity verified! Please set a new PIN or Pattern.", Toast.LENGTH_LONG).show()
                            } else {
                                forgotAnswerError = true
                            }
                        }) {
                            Text("Verify & Reset")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
                    }
                )
            }
        } else {
            // STEP C: Vault Dashboard (Unlocked)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Safe Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${vaultItems.size} Hidden items (Sandboxed & Hidden from File Manager)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { showVaultSecuritySettings = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Vault Security Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { isUnlocked = false },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                hideMediaQuery = ""
                                hideMediaTab = selectedTab.coerceIn(0, 1)
                                showHideMediaDialog = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Hide Files",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Tabs: Audios, Videos, Photos
                val audios = vaultItems.filter { it.mediaType == "AUDIO" }
                val videos = vaultItems.filter { it.mediaType == "VIDEO" }
                val photos = vaultItems.filter { it.mediaType == "PHOTO" }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Audios (${audios.size})") },
                        icon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Videos (${videos.size})") },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Photos (${photos.size})") },
                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                val currentList = when (selectedTab) {
                    0 -> audios
                    1 -> videos
                    else -> photos
                }

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No hidden ${when(selectedTab) { 0 -> "audios"; 1 -> "videos"; else -> "photos" }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the + button to hide media from the app or device into Safe Vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                hideMediaQuery = ""
                                hideMediaTab = selectedTab.coerceIn(0, 1)
                                showHideMediaDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hide Items Now")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentList, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (item.mediaType) {
                                                "VIDEO" -> Icons.Default.Videocam
                                                "PHOTO" -> Icons.Default.Image
                                                else -> Icons.Default.Audiotrack
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${TimeUtils.formatBytes(item.sizeBytes)}${if (item.durationMs > 0) " • ${TimeUtils.formatMs(item.durationMs)}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    var showItemMenu by remember { mutableStateOf(false) }

                                    // Play/Preview button
                                    if (item.mediaType == "PHOTO") {
                                        IconButton(onClick = { previewPhotoUri = item.mediaUri }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "View Photo", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                val track = TrackEntity(
                                                    id = item.id,
                                                    title = item.title,
                                                    artist = if (item.originalArtist.isNotBlank()) item.originalArtist else "Safe Vault",
                                                    album = "Safe Vault Locker",
                                                    durationMs = item.durationMs,
                                                    mediaUri = item.mediaUri,
                                                    isVideo = item.mediaType == "VIDEO",
                                                    fileSizeBytes = item.sizeBytes
                                                )
                                                onPlayVaultTrack(track)
                                            }
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Box {
                                        IconButton(onClick = { showItemMenu = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showItemMenu,
                                            onDismissRequest = { showItemMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Unhide & Restore to Device") },
                                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                onClick = {
                                                    showItemMenu = false
                                                    onRestoreItem(item)
                                                    Toast.makeText(context, "Restored back to phone storage & Library", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showItemMenu = false
                                                    onDeleteItemPermanently(item)
                                                    Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Photo Preview Dialog
            if (previewPhotoUri != null) {
                AlertDialog(
                    onDismissRequest = { previewPhotoUri = null },
                    confirmButton = {
                        TextButton(onClick = { previewPhotoUri = null }) { Text("Close") }
                    },
                    text = {
                        AsyncImage(
                            model = previewPhotoUri,
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                )
            }

            // Unified Hide Media Dialog (App Audio, App Videos, Device Storage)
            if (showHideMediaDialog) {
                AlertDialog(
                    onDismissRequest = { showHideMediaDialog = false },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hide Files into Safe Vault", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Files moved to encrypted private storage & erased from File Manager",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontSize = 11.sp
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TabRow(
                                selectedTabIndex = hideMediaTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            ) {
                                Tab(
                                    selected = hideMediaTab == 0,
                                    onClick = { hideMediaTab = 0 },
                                    text = { Text("Songs (${availableAudioTracks.size})", fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = hideMediaTab == 1,
                                    onClick = { hideMediaTab = 1 },
                                    text = { Text("Videos (${availableVideoTracks.size})", fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = hideMediaTab == 2,
                                    onClick = { hideMediaTab = 2 },
                                    text = { Text("Phone Files", fontSize = 12.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            when (hideMediaTab) {
                                0 -> {
                                    val filteredAudios = availableAudioTracks.filter {
                                        hideMediaQuery.isBlank() ||
                                                it.title.contains(hideMediaQuery, ignoreCase = true) ||
                                                it.artist.contains(hideMediaQuery, ignoreCase = true)
                                    }
                                    OutlinedTextField(
                                        value = hideMediaQuery,
                                        onValueChange = { hideMediaQuery = it },
                                        placeholder = { Text("Search songs in app...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (filteredAudios.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(220.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No songs available to hide", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().height(280.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(filteredAudios, key = { it.id }) { track ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Audiotrack,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = track.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${track.artist} • ${TimeUtils.formatMs(track.durationMs)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            onHideAppTrackToVault(track)
                                                            Toast.makeText(context, "'${track.title}' hidden in Safe Vault & erased from File Manager", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Hide")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    val filteredVideos = availableVideoTracks.filter {
                                        hideMediaQuery.isBlank() || it.title.contains(hideMediaQuery, ignoreCase = true)
                                    }
                                    OutlinedTextField(
                                        value = hideMediaQuery,
                                        onValueChange = { hideMediaQuery = it },
                                        placeholder = { Text("Search videos in app...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (filteredVideos.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(220.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No videos available to hide", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().height(280.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(filteredVideos, key = { it.id }) { video ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Videocam,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = video.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${TimeUtils.formatBytes(video.fileSizeBytes)}${if (video.durationMs > 0) " • ${TimeUtils.formatMs(video.durationMs)}" else ""}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            onHideAppTrackToVault(video)
                                                            Toast.makeText(context, "'${video.title}' hidden in Safe Vault & erased from File Manager", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Hide")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showHideMediaDialog = false
                                                photoPicker.launch("image/*")
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38BDF8))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("Import Photos from Device", fontWeight = FontWeight.SemiBold)
                                                    Text("Hide sensitive photos from device gallery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showHideMediaDialog = false
                                                videoPicker.launch("video/*")
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFF59E0B))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("Browse Device Videos", fontWeight = FontWeight.SemiBold)
                                                    Text("Hide video files from phone file storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showHideMediaDialog = false
                                                audioPicker.launch("audio/*")
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFFA855F7))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("Browse Device Audios", fontWeight = FontWeight.SemiBold)
                                                    Text("Hide audio recordings or mp3 from file storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHideMediaDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            // Vault Security Settings Dialog
            if (showVaultSecuritySettings) {
                var newQuestionInput by remember { mutableStateOf(prefs.getString("security_question", securityQuestions[0]) ?: securityQuestions[0]) }
                var newAnswerInput by remember { mutableStateOf("") }
                var showSettingsQuestionDropdown by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showVaultSecuritySettings = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vault Security Settings", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Current Security Question:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = prefs.getString("security_question", "What is your childhood nickname?") ?: "What is your childhood nickname?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text("Update Security Question:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { showSettingsQuestionDropdown = true }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(newQuestionInput, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = showSettingsQuestionDropdown,
                                    onDismissRequest = { showSettingsQuestionDropdown = false }
                                ) {
                                    securityQuestions.filter { it != "Custom Question..." }.forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q, fontSize = 13.sp) },
                                            onClick = {
                                                newQuestionInput = q
                                                showSettingsQuestionDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Text("New Security Answer:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newAnswerInput,
                                onValueChange = { newAnswerInput = it },
                                placeholder = { Text("Enter new secret answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (newAnswerInput.trim().isNotBlank()) {
                                        prefs.edit()
                                            .putString("security_question", newQuestionInput)
                                            .putString("security_answer", newAnswerInput.trim().lowercase())
                                            .apply()
                                        Toast.makeText(context, "Security question & answer updated!", Toast.LENGTH_SHORT).show()
                                        showVaultSecuritySettings = false
                                    } else {
                                        Toast.makeText(context, "Please enter an answer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Security Question")
                            }

                            Button(
                                onClick = {
                                    showVaultSecuritySettings = false
                                    prefs.edit().remove("secret").remove("lock_type").apply()
                                    isUnlocked = false
                                    isSetupMode = true
                                    setupStep = 1
                                    setupInput = ""
                                    setupConfirmInput = ""
                                    Toast.makeText(context, "Please set a new PIN or Pattern", Toast.LENGTH_SHORT).show()
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Change PIN or Pattern Lock")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showVaultSecuritySettings = false }) { Text("Close") }
                    }
                )
            }
        }
    }
}

@Composable
fun PinDotsRow(filledCount: Int, total: Int, isError: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0 until total) {
            val filled = i < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isError) MaterialTheme.colorScheme.error
                        else if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun NumericKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        digits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { item ->
                    if (item.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else if (item == "DEL") {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { onBackspace() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDigit(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Pattern3x3Grid(
    selectedSequence: String,
    onDotClicked: (Int) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (col in 0..2) {
                    val dotIndex = row * 3 + col + 1
                    val isSelected = selectedSequence.contains(dotIndex.toString())
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onDotClicked(dotIndex) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClear) {
            Text("Clear Pattern")
        }
    }
}
