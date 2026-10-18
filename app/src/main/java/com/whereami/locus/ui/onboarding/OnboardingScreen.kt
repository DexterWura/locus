package com.whereami.locus.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.whereami.locus.privacy.PrivacyUrls
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.whereami.locus.auth.PasswordCrypto
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
    var confirmPhrase by remember { mutableStateOf("") }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            permissionStep = 1
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionStep = 2
    }

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
                            confirmPhrase = ""
                            phase = OnboardingPhase.RecoveryPhrase
                        }
                    }
                )
            }

            OnboardingPhase.RecoveryPhrase -> {
                RecoveryPhraseStep(
                    phrase = recoveryPhrase,
                    confirmPhrase = confirmPhrase,
                    error = authError,
                    onConfirmPhraseChange = {
                        confirmPhrase = it
                        onClearAuthError()
                    },
                    onContinue = {
                        val matches = PasswordCrypto.normalizePhrase(confirmPhrase) ==
                            PasswordCrypto.normalizePhrase(recoveryPhrase)
                        if (!matches) return@RecoveryPhraseStep
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
                    onFinish = onFinished,
                    onSkipNotifications = { permissionStep = 2 },
                    onOpenPrivacy = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyUrls.POLICY))
                        )
                    },
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
    confirmPhrase: String,
    error: String?,
    onConfirmPhraseChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val matches = PasswordCrypto.normalizePhrase(confirmPhrase) ==
        PasswordCrypto.normalizePhrase(phrase)
    val mismatch = confirmPhrase.isNotBlank() && !matches

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Save your recovery phrase",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Write these words down somewhere safe offline. Then type them below to confirm. Locus cannot show this phrase again.",
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
        OutlinedTextField(
            value = confirmPhrase,
            onValueChange = onConfirmPhraseChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Type the recovery phrase") },
            placeholder = { Text("word word word …") },
            minLines = 2,
            maxLines = 4,
            enabled = true,
            readOnly = false,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            ),
            isError = mismatch || error != null,
            supportingText = {
                when {
                    error != null -> Text(error)
                    mismatch -> Text("Phrase does not match yet")
                    matches -> Text("Phrase matches")
                    else -> Text("Enter all eight words, separated by spaces")
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            enabled = matches,
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
    onFinish: () -> Unit,
    onSkipNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val (title, body, primary) = when (step) {
        0 -> Triple(
            "Precise location",
            "Locus uses precise location to draw your day trail on this phone. Map tiles may load near the area you view. Deny only if you want to explore the app without recording.",
            "Allow precise location"
        )
        1 -> Triple(
            "Allow notifications",
            "Needed if you later turn on all-day background tracking, so Android can show the required ongoing notice. You can skip this for now.",
            "Allow notifications"
        )
        else -> Triple(
            "You’re ready",
            "Tracking defaults to while you use the app — no sticky notice. All-day background location is optional later in Settings, with a clear disclosure first. Read the privacy policy anytime.",
            "Start using Locus"
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
                    else -> onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(primary)
        }
        if (step == 0) {
            TextButton(onClick = onOpenAppSettings) {
                Text("Open app settings")
            }
        }
        if (step == 1) {
            OutlinedButton(
                onClick = onSkipNotifications,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Skip notifications")
            }
        }
        if (step >= 2) {
            TextButton(onClick = onOpenPrivacy) {
                Text("Privacy policy")
            }
        }
    }
}
