package com.priyanshu.localplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.priyanshu.localplayer.R
import com.priyanshu.localplayer.ui.utils.formatTime

@Composable
fun NowPlayingPolished(
    title: String,
    artist: String,
    album: String,
    albumArtUri: Uri?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🎵 ALBUM ART (tap → queue)
        AsyncImage(
            model = albumArtUri ?: R.drawable.ic_music_placeholder,
            contentDescription = null,
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onQueue() },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 🎶 INFO
        Text(text = title, color = Color.White)
        Text(text = artist, color = Color.Gray)
        Text(text = album, color = Color.Gray)

        Spacer(modifier = Modifier.height(20.dp))

        // ⏱ SEEK
        Slider(
            value = position.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(position), color = Color.White)
            Text(formatTime(duration), color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🔀 ⏮ ▶ ⏭ 🔁 CONTROLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "🔀",
                color = if (isShuffleEnabled) Color(0xFFBB86FC) else Color.White,
                modifier = Modifier.clickable { onShuffle() }
            )

            Text(
                text = "⏮",
                color = Color.White,
                modifier = Modifier.clickable { onPrev() }
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DB954))
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    color = Color.Black
                )
            }

            Text(
                text = "⏭",
                color = Color.White,
                modifier = Modifier.clickable { onNext() }
            )

            Text(
                text = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> "🔂"
                    Player.REPEAT_MODE_ALL -> "🔁"
                    else -> "➡"
                },
                color = if (repeatMode != Player.REPEAT_MODE_OFF)
                    Color(0xFFBB86FC) else Color.White,
                modifier = Modifier.clickable { onRepeat() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📜 QUEUE
        Text(
            text = "Queue",
            color = Color.Gray,
            modifier = Modifier.clickable { onQueue() }
        )
    }
}
