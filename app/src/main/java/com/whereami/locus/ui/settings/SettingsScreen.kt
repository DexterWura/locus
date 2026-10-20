package com.whereami.locus.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.whereami.locus.data.CompanyLink
import com.whereami.locus.domain.TrackingMode
import com.whereami.locus.tracking.LocationPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    trackingEnabled: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    trackingMode: TrackingMode,
    onTrackingModeChange: (TrackingMode) -> Unit,
    sampleIntervalMs: Long,
    onSampleIntervalChange: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    companyLink: CompanyLink?,
    companyLinkBusy: Boolean,
    companyLinkMessage: String?,
    onClearCompanyLinkMessage: () -> Unit,
    onPairFromQr: (String) -> Unit,
    onUnlinkCompany: () -> Unit,
    authError: String?,
    onClearAuthError: () -> Unit,
    onChangePassword: (
        current: String,
        newPassword: String,
        confirm: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmUnlink by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showBackgroundDisclosure by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordChanged by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            onPairFromQr(contents)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scan the company phone QR from Device Management")
                    setBeepEnabled(false)
                    setOrientationLocked(true)
                }
            )
        }
    }
    val options = listOf(
        5_000L to "Every 5 seconds (default)",
        10_000L to "Every 10 seconds",
        30_000L to "Every 30 seconds"
    )

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onTrackingModeChange(TrackingMode.Background)
        }
    }
    val fineThenBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                onTrackingModeChange(TrackingMode.Background)
            }
        }
    }

    fun requestBackgroundAccess() {
        when {
            LocationPermissions.hasBackground(context) -> {
                onTrackingModeChange(TrackingMode.Background)
            }
            !LocationPermissions.hasFine(context) -> {
                fineThenBackgroundLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            else -> onTrackingModeChange(TrackingMode.Background)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "While using the app records only when Locus is open — no sticky notice. All day keeps recording in the background; Android requires an ongoing notice for that.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (trackingEnabled) "Tracking on" else "Tracking off",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = trackingEnabled,
                            onCheckedChange = onToggleTracking
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.selectableGroup()) {
                        RowSelectable(
                            selected = trackingMode == TrackingMode.Foreground,
                            label = "While using the app (no notification)",
                            onClick = { onTrackingModeChange(TrackingMode.Foreground) }
                        )
                        RowSelectable(
                            selected = trackingMode == TrackingMode.Background,
                            label = "All day in background (ongoing notice)",
                            onClick = {
                                if (LocationPermissions.hasBackground(context)) {
                                    onTrackingModeChange(TrackingMode.Background)
                                } else {
                                    showBackgroundDisclosure = true
                                }
                            }
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Sampling",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "How often Locus asks for a precise fix while you are moving. Still periods automatically relax to 30 seconds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.selectableGroup()) {
                        options.forEach { (value, label) ->
                            RowSelectable(
                                selected = sampleIntervalMs == value,
                                label = label,
                                onClick = { onSampleIntervalChange(value) }
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Company link",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlinked phones keep trails only on this device. After you scan a Device Management QR, Locus still saves locally and also sends new points to that company.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (companyLink != null) {
                        Text(
                            text = "Linked to ${companyLink.assetName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { confirmUnlink = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !companyLinkBusy,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Unlink company")
                        }
                    } else {
                        Button(
                            onClick = {
                                onClearCompanyLinkMessage()
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !companyLinkBusy,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (companyLinkBusy) "Linking…" else "Scan company QR")
                        }
                    }
                    if (companyLinkMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = companyLinkMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your password and recovery options live only on this phone. If you forget both your password and recovery phrase, and cannot answer your security questions, Locus cannot reset access for you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            passwordChanged = false
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            onClearAuthError()
                            showChangePassword = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Change password")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Privacy & security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "By default your trail stays on this phone. The database is encrypted (SQLCipher + Android Keystore) and cloud backup is disabled. If you link to a company via QR, new points are also sent to that company’s Device Management. Map tiles come from OpenFreeMap over HTTPS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenPrivacy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Privacy policy")
                    }
                }
            }

            Button(
                onClick = { confirmDelete = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete all trail data")
            }
        }
    }

    if (showBackgroundDisclosure) {
        AlertDialog(
            onDismissRequest = { showBackgroundDisclosure = false },
            title = { Text("Background location") },
            text = {
                Text(
                    "Locus collects location data to record your day trail even when the app is closed or not in use. " +
                        "Data stays on this phone. Map tiles may load near your position over HTTPS. " +
                        "An ongoing notification stays visible while all-day tracking is on."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundDisclosure = false
                        requestBackgroundAccess()
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundDisclosure = false }) { Text("Not now") }
            }
        )
    }

    if (confirmUnlink) {
        AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text("Unlink company?") },
            text = {
                Text("Locus will stop sending locations to the company. Trails already on this phone stay here.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUnlinkCompany()
                        confirmUnlink = false
                    }
                ) { Text("Unlink") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnlink = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete everything?") },
            text = {
                Text("This removes every recorded point and named place from this phone. It cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAll()
                        confirmDelete = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangePassword) {
        AlertDialog(
            onDismissRequest = { showChangePassword = false },
            title = { Text("Change password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            onClearAuthError()
                            passwordChanged = false
                        },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            onClearAuthError()
                            passwordChanged = false
                        },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            onClearAuthError()
                            passwordChanged = false
                        },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (authError != null) {
                        Text(authError, color = MaterialTheme.colorScheme.error)
                    }
                    if (passwordChanged && authError == null) {
                        Text(
                            "Password updated.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChangePassword(
                            currentPassword,
                            newPassword,
                            confirmPassword
                        ) {
                            passwordChanged = true
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePassword = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun RowSelectable(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
