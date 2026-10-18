package com.whereami.locus.ui.history

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whereami.locus.domain.DayDetail
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.TimelineItem
import com.whereami.locus.ui.components.BottomSheetCard
import com.whereami.locus.ui.components.RoundIconButton
import com.whereami.locus.ui.map.LocusMap

@Composable
fun DayDetailScreen(
    detail: DayDetail?,
    onBack: () -> Unit,
    onNameStop: (LatLng, String) -> Unit
) {
    var namingStop by remember { mutableStateOf<TimelineItem.Stop?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (detail != null) {
            LocusMap(
                path = detail.drawablePoints,
                currentPosition = detail.drawablePoints.lastOrNull(),
                animatePath = true,
                fitToPath = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        RoundIconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }

        BottomSheetCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = detail?.let { DayTime.formatDayTitle(it.dayStartMillis) } ?: "Day",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail?.let { DayTime.formatDistance(it.distanceMeters) } ?: "—",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                when {
                    detail == null -> Text("Loading…")
                    detail.timeline.isEmpty() -> Text(
                        text = "Not enough points for a timeline yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(detail.timeline) { item ->
                            when (item) {
                                is TimelineItem.Stop -> StopRow(
                                    stop = item,
                                    onName = { namingStop = item }
                                )
                                is TimelineItem.Trip -> TripRow(trip = item)
                            }
                        }
                    }
                }
            }
        }
    }

    namingStop?.let { stop ->
        var name by remember(stop) { mutableStateOf(stop.placeName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { namingStop = null },
            title = { Text("Name this place") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Place name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNameStop(stop.center, name)
                        namingStop = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { namingStop = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StopRow(
    stop: TimelineItem.Stop,
    onName: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = stop.placeName ?: "Unnamed stop",
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${DayTime.formatClock(stop.arrivedAt)} – ${DayTime.formatClock(stop.departedAt)} · ${
                    DayTime.formatDuration(stop.departedAt - stop.arrivedAt)
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onName) {
            Text(if (stop.placeName == null) "Name" else "Rename")
        }
    }
}

@Composable
private fun TripRow(trip: TimelineItem.Trip) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Route,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "Trip · ${DayTime.formatDistance(trip.distanceMeters)}",
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${DayTime.formatClock(trip.startedAt)} – ${DayTime.formatClock(trip.endedAt)} · ${
                    DayTime.formatDuration(trip.endedAt - trip.startedAt)
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
