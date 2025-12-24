package com.priyanshu.localplayer.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // 🎨 Smooth Slider Logic
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    val animatedPosition = remember { Animatable(position.toFloat()) }

    LaunchedEffect(position, isPlaying, isDragging) {
        if (!isDragging) {
            if (isPlaying) {
                animatedPosition.animateTo(
                    targetValue = position.toFloat(),
                    animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                )
            } else {
                animatedPosition.snapTo(position.toFloat())
            }
        }
    }

    val displayPosition = if (isDragging) dragPosition else animatedPosition.value

    // ✨ Play/Pause Button Animation Logic
    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 37.5.dp, 
        animationSpec = tween(durationMillis = 400),
        label = "play_pause_shape"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 🖼️ ALBUM ART
        AsyncImage(
            model = albumArtUri ?: R.drawable.ic_music_placeholder,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ℹ️ SONG INFO
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ⏱️ PROGRESS BAR
        Slider(
            value = displayPosition.coerceIn(0f, if (duration > 0) duration.toFloat() else 1f),
            onValueChange = { 
                isDragging = true
                dragPosition = it 
            },
            onValueChangeFinished = {
                onSeek(dragPosition.toLong())
                isDragging = false
            },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(displayPosition.toLong()), color = Color.Gray, fontSize = 12.sp)
            Text(text = formatTime(duration), color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🎮 CONTROLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRepeat) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFF1DB954) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onPrev) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Fixed the ugly square outline by using Surface's onClick and clipping
            Surface(
                onClick = onPlayPause, // ✅ Use Surface's onClick for shape-aware ripple
                modifier = Modifier
                    .size(75.dp)
                    .clip(RoundedCornerShape(cornerRadius)), // ✅ Clip the modifier to match shape
                shape = RoundedCornerShape(cornerRadius), 
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(onClick = onShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = if (isShuffleEnabled) Color(0xFF1DB954) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 📜 QUEUE HANDLE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onQueue() }
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Queue",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}
