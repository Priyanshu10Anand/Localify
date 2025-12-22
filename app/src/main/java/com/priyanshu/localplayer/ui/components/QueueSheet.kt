@file:OptIn(ExperimentalMaterial3Api::class)

package com.priyanshu.localplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priyanshu.localplayer.data.model.Song

@Composable
fun QueueSheet(
    songs: List<Song>,
    currentIndex: Int,
    onSongSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212)
    ) {
        Text(
            text = "Queue",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            itemsIndexed(songs) { index, song ->
                val isCurrent = index == currentIndex

                Text(
                    text = song.title,
                    color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
