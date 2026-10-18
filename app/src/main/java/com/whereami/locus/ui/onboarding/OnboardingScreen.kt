package com.whereami.locus.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whereami.locus.auth.SecurityQuestions
import com.whereami.locus.presentation.AuthViewModel
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val icon: ImageVector
)

private enum class OnboardingPhase {
    Intro,
    Password,
    Questions,
    RecoveryPhrase,
    Permissions
}

@Composable
fun OnboardingScreen(
    authError: String?,
    onClearAuthError: () -> Unit,
    onGenerateRecoveryPhrase: () -> String,
    onSetupAuth: (
        password: String,
        confirm: String,
        question1Id: String,
        question2Id: String,
        answer1: String,
        answer2: String,
        recoveryPhrase: String,
        onDone: () -> Unit
    ) -> Unit,
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Your day stays here",
            body = "Locus records your path only on this phone. There is no account and nothing is uploaded.",
            icon = Icons.Outlined.Lock
        ),
        OnboardingPage(
            title = "Precise location draws the line",
            body = "Locus uses precise GPS to trace where you go and when. The map tiles are the only thing that needs the network.",
            icon = Icons.Outlined.MyLocation
        ),
        OnboardingPage(
            title = "A quiet notice while tracking",
            body = "While recording, Android shows a low-priority notification so you always know Locus is keeping today’s path.",
            icon = Icons.Outlined.NotificationsActive
        ),
        OnboardingPage(
            title = "Lock your trail",
            body = "Next you’ll create a password and local recovery options. Nothing leaves this phone — if you forget both, Locus cannot email a reset.",
            icon = Icons.Outlined.Key
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(OnboardingPhase.Intro) }
    var permissionStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var question1Id by remember { mutableStateOf(SecurityQuestions.all[0].id) }
    var question2Id by remember { mutableStateOf(SecurityQuestions.all[1].id) }
    var answer1 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }
    var recoveryPhrase by remember { mutableStateOf("") }
    var wrotePhraseDown by remember { mutableStateOf(false) }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionStep = 1 }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionStep = 2 }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionStep = 3 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (phase) {
            OnboardingPhase.Intro -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnboardingPageContent(pages[page])
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            phase = OnboardingPhase.Password
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (pagerState.currentPage < pages.lastIndex) "Next" else "Create password")
                }
            }

            OnboardingPhase.Password -> {
                PasswordStep(
                    password = password,
                    confirm = confirm,
                    error = authError,
                    onPasswordChange = { password = it; onClearAuthError() },
                    onConfirmChange = { confirm = it; onClearAuthError() },
                    onContinue = {
                        if (AuthViewModel.validatePassword(password, confirm) == null) {
                            phase = OnboardingPhase.Questions
                        }
                    },
                    localError = AuthViewModel.validatePassword(password, confirm)
                        .takeIf { password.isNotEmpty() || confirm.isNotEmpty() }
                )
            }

            OnboardingPhase.Questions -> {
                QuestionsStep(
                    question1Id = question1Id,
                    question2Id = question2Id,
                    answer1 = answer1,
                    answer2 = answer2,
                    onQuestion1 = { question1Id = it },
                    onQuestion2 = { question2Id = it },
                    onAnswer1 = { answer1 = it },
                    onAnswer2 = { answer2 = it },
                    onContinue = {
                        if (question1Id != question2Id &&
                            answer1.isNotBlank() &&
                            answer2.isNotBlank()
                        ) {
                            recoveryPhrase = onGenerateRecoveryPhrase()
                            wrotePhraseDown = false
                            phase = OnboardingPhase.RecoveryPhrase
                        }
                    }
                )
            }

            OnboardingPhase.RecoveryPhrase -> {
                RecoveryPhraseStep(
                    phrase = recoveryPhrase,
                    checked = wrotePhraseDown,
                    error = authError,
                    onChecked = { wrotePhraseDown = it },
                    onContinue = {
                        if (!wrotePhraseDown) return@RecoveryPhraseStep
                        onSetupAuth(
                            password,
                            confirm,
                            question1Id,
                            question2Id,
                            answer1,
                            answer2,
                            recoveryPhrase
                        ) {
                            phase = OnboardingPhase.Permissions
                            fineLocationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            OnboardingPhase.Permissions -> {
                PermissionStepContent(
                    step = permissionStep,
                    onRequestFineLocation = {
                        fineLocationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onRequestNotification = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissionStep = 2
                        }
                    },
                    onRequestBackground = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            permissionStep = 3
                        }
                    },
                    onOpenBatterySettings = {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            ).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                        onFinished()
                    },
                    onSkipBattery = onFinished,
                    onOpenAppSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun PasswordStep(
    password: String,
    confirm: String,
    error: String?,
    localError: String?,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val shownError = error ?: localError?.takeIf { confirm.isNotEmpty() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create a password",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This password unlocks Locus on this phone. It is stored only as a hash — never sent anywhere.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = onConfirmChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = shownError != null,
            supportingText = shownError?.let { { Text(it) } }
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onContinue,
            enabled = AuthViewModel.validatePassword(password, confirm) == null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Continue") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionsStep(
    question1Id: String,
    question2Id: String,
    answer1: String,
    answer2: String,
    onQuestion1: (String) -> Unit,
    onQuestion2: (String) -> Unit,
    onAnswer1: (String) -> Unit,
    onAnswer2: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Security questions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick two questions only you can answer. Answers stay hashed on this phone and are used if you forget your password.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        QuestionPicker(
            label = "Question 1",
            selectedId = question1Id,
            excludeId = question2Id,
            onSelected = onQuestion1
        )
        OutlinedTextField(
            value = answer1,
            onValueChange = onAnswer1,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Answer 1") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        QuestionPicker(
            label = "Question 2",
            selectedId = question2Id,
            excludeId = question1Id,
            onSelected = onQuestion2
        )
        OutlinedTextField(
            value = answer2,
            onValueChange = onAnswer2,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Answer 2") },
            singleLine = true
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onContinue,
            enabled = question1Id != question2Id &&
                answer1.isNotBlank() &&
                answer2.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Continue") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionPicker(
    label: String,
    selectedId: String,
    excludeId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = SecurityQuestions.byId(selectedId)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.prompt.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SecurityQuestions.all.filter { it.id != excludeId }.forEach { question ->
                DropdownMenuItem(
                    text = { Text(question.prompt) },
                    onClick = {
                        onSelected(question.id)
                        expanded = false
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RecoveryPhraseStep(
    phrase: String,
    checked: Boolean,
    error: String?,
    onChecked: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Write down your recovery phrase",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This phrase is shown once. Store it somewhere safe offline. Locus cannot show it again or send it to you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Text(
                text = phrase,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text("I wrote this phrase down somewhere safe")
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            enabled = checked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Save and continue") }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionStepContent(
    step: Int,
    onRequestFineLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBackground: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSkipBattery: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val (title, body, primary) = when (step) {
        0 -> Triple(
            "Precise location",
            "Waiting for the system permission. If the dialog closed, tap below to ask again.",
            "Allow precise location"
        )
        1 -> Triple(
            "Allow notifications",
            "Android needs this so Locus can show the ongoing tracking notice. It stays quiet and low priority.",
            "Allow notifications"
        )
        2 -> Triple(
            "Allow all-the-time location",
            "Background location lets Locus keep the day trail when the screen is off. Android opens a settings screen for this.",
            "Open location settings"
        )
        else -> Triple(
            "Optional: ignore battery limits",
            "Some phones pause GPS apps aggressively. You can allow Locus to ignore battery optimizations for more reliable all-day tracking.",
            "Allow for Locus"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                when (step) {
                    0 -> onRequestFineLocation()
                    1 -> onRequestNotification()
                    2 -> onRequestBackground()
                    else -> onOpenBatterySettings()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(primary)
        }
        if (step == 2) {
            TextButton(onClick = onOpenAppSettings) {
                Text("Open app settings instead")
            }
        }
        if (step >= 3) {
            OutlinedButton(
                onClick = onSkipBattery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Skip for now")
            }
        }
    }
}
