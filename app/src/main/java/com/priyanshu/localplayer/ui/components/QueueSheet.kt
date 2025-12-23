package com.priyanshu.localplayer.ui.components

import androidx.compose.foundation.background
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
import com.priyanshu.localplayer.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
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
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(songs) { index, song ->

                val isCurrent = index == currentIndex

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelected(index) }
                        .background(
                            if (isCurrent) Color(0xFF1E1E1E)
                            else Color.Transparent
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = song.title,
                        color = if (isCurrent) Color(0xFFBB86FC) else Color.White
                    )
                    Text(
                        text = song.artist,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
