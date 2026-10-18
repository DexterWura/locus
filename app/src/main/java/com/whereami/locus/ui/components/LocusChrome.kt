package com.whereami.locus.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 3.dp,
        tonalElevation = 0.dp
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp), content = content)
    }
}

@Composable
fun BottomSheetCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        content = { content() }
    )
}
