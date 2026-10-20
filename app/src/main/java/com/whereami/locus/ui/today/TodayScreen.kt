package com.whereami.locus.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.Heading
import com.whereami.locus.domain.TrackingStatus
import com.whereami.locus.presentation.TodayUiState
import com.whereami.locus.ui.components.BottomSheetCard
import com.whereami.locus.ui.components.CompassDial
import com.whereami.locus.ui.components.RoundIconButton
import com.whereami.locus.ui.map.LocusMap
import com.whereami.locus.ui.theme.LocatorBlue
import com.whereami.locus.ui.theme.MovingGreen
import com.whereami.locus.ui.theme.StillAmber

@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var recenterToken by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        LocusMap(
            pathSegments = state.pathSegments,
            currentPosition = state.currentPosition,
            animatePath = true,
            fitToPath = state.path.size >= 2,
            recenterToken = recenterToken,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoundIconButton(onClick = onOpenHistory) {
                Icon(Icons.Outlined.History, contentDescription = "History")
            }
            RoundIconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        }

        FloatingActionButton(
            onClick = { recenterToken++ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 268.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = "Recenter")
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            TodaySheet(state = state)
        }
    }
}

@Composable
private fun TodaySheet(state: TodayUiState) {
    BottomSheetCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor(state.status))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = statusLabel(state.status),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusSubtitle(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.headingDegrees?.let { heading ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CompassDial(headingDegrees = heading)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Heading.cardinal(heading),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (state.points.isNotEmpty() || state.headingDegrees != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (state.points.isNotEmpty()) {
                        Metric(
                            label = "From",
                            value = state.firstPointAt?.let(DayTime::formatClock) ?: "—"
                        )
                        Metric(
                            label = "Last fix",
                            value = state.lastPointAt?.let(DayTime::formatClock) ?: "—"
                        )
                    }
                    Metric(
                        label = "Facing",
                        value = state.headingDegrees?.let(Heading::format)
                            ?: if (state.compassAvailable) "…" else "N/A"
                    )
                    if (state.points.isNotEmpty()) {
                        Metric(
                            label = "Accuracy",
                            value = state.lastAccuracyMeters?.let { String.format("%.0f m", it) }
                                ?: "—"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun statusLabel(status: TrackingStatus): String = when (status) {
    TrackingStatus.Moving -> "Moving"
    TrackingStatus.Still -> "Still"
    TrackingStatus.Paused -> "Paused"
}

private fun statusSubtitle(state: TodayUiState): String {
    if (!state.trackingEnabled) {
        return if (state.points.isEmpty()) {
            "Turn on tracking in Settings to begin today’s path"
        } else {
            "${DayTime.formatDistance(state.distanceMeters)} today · off"
        }
    }
    val distance = DayTime.formatDistance(state.distanceMeters)
    val speed = state.speedMps?.takeIf { it >= 0f }?.let { formatSpeed(it) }
    val facing = state.headingDegrees?.let { Heading.cardinal(it) }
    return when (state.status) {
        TrackingStatus.Moving -> buildString {
            append(distance)
            append(" today")
            if (speed != null) append(" · $speed")
            if (facing != null) append(" · $facing")
        }
        TrackingStatus.Still -> buildString {
            append(distance)
            append(" today · stationary")
            if (facing != null) append(" · facing $facing")
        }
        TrackingStatus.Paused -> "$distance today · paused"
    }
}

private fun formatSpeed(metersPerSecond: Float): String {
    val kmh = metersPerSecond * 3.6f
    return if (kmh < 1f) {
        String.format("%.1f m/s", metersPerSecond)
    } else {
        String.format("%.1f km/h", kmh)
    }
}

private fun statusColor(status: TrackingStatus): Color = when (status) {
    TrackingStatus.Moving -> MovingGreen
    TrackingStatus.Still -> StillAmber
    TrackingStatus.Paused -> LocatorBlue.copy(alpha = 0.45f)
}
