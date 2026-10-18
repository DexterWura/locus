package com.whereami.locus.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.whereami.locus.auth.SecurityQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    questions: Pair<SecurityQuestion, SecurityQuestion>?,
    recoveryAuthorized: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onLoadQuestions: () -> Unit,
    onRecoverWithAnswers: (String, String) -> Unit,
    onRecoverWithPhrase: (String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onLoadQuestions() }

    var mode by remember { mutableIntStateOf(0) }
    var answer1 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }
    var phrase by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recoveryAuthorized) "New password" else "Recover access") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!recoveryAuthorized) {
                Text(
                    text = "Everything stays on this phone. Use the security questions you chose, or the recovery phrase you wrote down during setup.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == 0,
                        onClick = { mode = 0; onClearError() },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Questions") }
                    SegmentedButton(
                        selected = mode == 1,
                        onClick = { mode = 1; onClearError() },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Phrase") }
                }
                if (mode == 0) {
                    val q = questions
                    if (q == null) {
                        Text("Loading questions…")
                    } else {
                        Text(q.first.prompt, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = answer1,
                            onValueChange = { answer1 = it; onClearError() },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Answer") },
                            singleLine = true
                        )
                        Text(q.second.prompt, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = answer2,
                            onValueChange = { answer2 = it; onClearError() },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Answer") },
                            singleLine = true
                        )
                        if (error != null) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onRecoverWithAnswers(answer1, answer2) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Verify answers") }
                    }
                } else {
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { phrase = it; onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Recovery phrase") },
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None
                        )
                    )
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onRecoverWithPhrase(phrase) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Verify phrase") }
                }
            } else {
                Text(
                    text = "Choose a new password. Your trail data was never uploaded, so recovery only works with what you set up on this phone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { onResetPassword(newPassword, confirm) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Save new password") }
            }
        }
    }
}
