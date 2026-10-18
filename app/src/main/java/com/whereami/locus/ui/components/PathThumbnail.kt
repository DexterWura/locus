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

@Composable
fun PathThumbnail(
    points: List<LatLng>,
    modifier: Modifier = Modifier,
    color: Color = LocatorBlue
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.0001)
        val padding = 12f
        val width = size.width - padding * 2
        val height = size.height - padding * 2

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = padding + (((point.longitude - minLon) / lonSpan) * width).toFloat()
            val y = padding + (((maxLat - point.latitude) / latSpan) * height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val last = points.last()
        val lx = padding + (((last.longitude - minLon) / lonSpan) * width).toFloat()
        val ly = padding + (((maxLat - last.latitude) / latSpan) * height).toFloat()
        drawCircle(color = color, radius = 6f, center = Offset(lx, ly))
    }
}
