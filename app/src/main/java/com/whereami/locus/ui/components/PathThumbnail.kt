package com.whereami.locus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.whereami.locus.domain.LatLng
import com.whereami.locus.ui.theme.LocatorBlue
import kotlin.math.cos

@Composable
fun PathThumbnail(
    points: List<LatLng>,
    modifier: Modifier = Modifier,
    color: Color = LocatorBlue,
    segments: List<List<LatLng>> = emptyList()
) {
    val drawSegments = if (segments.isNotEmpty()) {
        segments
    } else if (points.isNotEmpty()) {
        listOf(points)
    } else {
        emptyList()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val allPoints = drawSegments.flatten()
        if (allPoints.size < 2) return@Canvas

        val midLatRad = Math.toRadians(allPoints.map { it.latitude }.average())
        val lonScale = cos(midLatRad).coerceAtLeast(0.2)

        val minLat = allPoints.minOf { it.latitude }
        val maxLat = allPoints.maxOf { it.latitude }
        val minLon = allPoints.minOf { it.longitude }
        val maxLon = allPoints.maxOf { it.longitude }
        val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonSpan = ((maxLon - minLon) * lonScale).coerceAtLeast(0.0001)
        val padding = 12f
        val width = size.width - padding * 2
        val height = size.height - padding * 2

        fun project(point: LatLng): Offset {
            val x = padding + ((((point.longitude - minLon) * lonScale) / lonSpan) * width).toFloat()
            val y = padding + (((maxLat - point.latitude) / latSpan) * height).toFloat()
            return Offset(x, y)
        }

        for (segment in drawSegments) {
            if (segment.size < 2) continue
            val path = Path()
            segment.forEachIndexed { index, point ->
                val offset = project(point)
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        val last = allPoints.last()
        val lastOffset = project(last)
        drawCircle(color = color, radius = 6f, center = lastOffset)
    }
}
