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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.PlaceLabel
import com.whereami.locus.domain.TimelineItem
import com.whereami.locus.ui.components.BottomSheetCard
import com.whereami.locus.ui.components.RoundIconButton
import com.whereami.locus.ui.map.LocusMap
import com.whereami.locus.ui.places.PlaceDetailDialog
import com.whereami.locus.ui.places.PlaceEditorDialog

private sealed class PlaceDialog {
    data class Create(val location: LatLng) : PlaceDialog()
    data class Detail(val place: PlaceLabel) : PlaceDialog()
    data class Edit(val place: PlaceLabel) : PlaceDialog()
    data class NameStop(val stop: TimelineItem.Stop) : PlaceDialog()
}

@Composable
fun DayDetailScreen(
    detail: DayDetail?,
    places: List<PlaceLabel>,
    onBack: () -> Unit,
    onNameStop: (LatLng, String, String?) -> Unit,
    onSavePlace: (latitude: Double, longitude: Double, name: String, note: String?) -> Unit,
    onUpdatePlace: (PlaceLabel, name: String, note: String?) -> Unit,
    onDeletePlace: (Long) -> Unit
) {
    var placeDialog by remember { mutableStateOf<PlaceDialog?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (detail != null) {
            LocusMap(
                pathSegments = detail.drawableSegments,
                currentPosition = detail.drawablePoints.lastOrNull(),
                places = places,
                animatePath = true,
                fitToPath = true,
                onMapLongClick = { latLng -> placeDialog = PlaceDialog.Create(latLng) },
                onPlaceClick = { place -> placeDialog = PlaceDialog.Detail(place) },
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
                                    onName = { placeDialog = PlaceDialog.NameStop(item) }
                                )
                                is TimelineItem.Trip -> TripRow(trip = item)
                            }
                        }
                    }
                }
            }
        }
    }

    when (val dialog = placeDialog) {
        is PlaceDialog.Create -> PlaceEditorDialog(
            title = "Name this place",
            onDismiss = { placeDialog = null },
            onSave = { name, note ->
                onSavePlace(dialog.location.latitude, dialog.location.longitude, name, note)
                placeDialog = null
            }
        )
        is PlaceDialog.NameStop -> PlaceEditorDialog(
            title = "Name this place",
            initialName = dialog.stop.placeName.orEmpty(),
            onDismiss = { placeDialog = null },
            onSave = { name, note ->
                onNameStop(dialog.stop.center, name, note)
                placeDialog = null
            }
        )
        is PlaceDialog.Detail -> {
            val distance = detail?.drawablePoints?.lastOrNull()?.let {
                PathGeometry.haversineMeters(
                    it.latitude,
                    it.longitude,
                    dialog.place.latitude,
                    dialog.place.longitude
                )
            }
            PlaceDetailDialog(
                place = dialog.place,
                distanceMeters = distance,
                onDismiss = { placeDialog = null },
                onRename = { placeDialog = PlaceDialog.Edit(dialog.place) },
                onDelete = {
                    onDeletePlace(dialog.place.id)
                    placeDialog = null
                }
            )
        }
        is PlaceDialog.Edit -> PlaceEditorDialog(
            title = "Rename place",
            initialName = dialog.place.name,
            initialNote = dialog.place.note,
            onDismiss = { placeDialog = PlaceDialog.Detail(dialog.place) },
            onSave = { name, note ->
                onUpdatePlace(dialog.place, name, note)
                placeDialog = null
            }
        )
        null -> Unit
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
