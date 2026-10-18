package com.whereami.locus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.whereami.locus.ui.theme.LocatorBlue

@Composable
fun CompassDial(
    headingDegrees: Float,
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.outline,
    needleColor: Color = LocatorBlue
) {
    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = ringColor.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 3f)
            )

            // Keep the dial labels fixed to the phone; rotate the needle to north.
            rotate(degrees = -headingDegrees, pivot = center) {
                val needle = Path().apply {
                    moveTo(center.x, center.y - radius + 8f)
                    lineTo(center.x - 7f, center.y + 4f)
                    lineTo(center.x, center.y - 2f)
                    lineTo(center.x + 7f, center.y + 4f)
                    close()
                }
                drawPath(needle, color = needleColor)

                drawCircle(
                    color = needleColor,
                    radius = 4f,
                    center = center
                )
            }

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 18f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - radius + 18f, labelPaint)
        }
    }
}
