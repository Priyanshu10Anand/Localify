package com.priyanshu.localplayer.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.priyanshu.localplayer.R
import com.priyanshu.localplayer.ui.utils.formatTime
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun NowPlayingPolished(
    songId: Long, // ✅ Added for unique transition tracking
    title: String,
    artist: String,
    album: String,
    albumArtUri: Uri?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit
) {
    // 🎨 Smooth Slider Logic
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    val animatedPosition = remember { Animatable(position.toFloat()) }

    LaunchedEffect(position, isPlaying, isDraggingSlider) {
        if (!isDraggingSlider) {
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

    val displayPosition = if (isDraggingSlider) dragPosition else animatedPosition.value

    // ✨ Play/Pause Button Animation Logic
    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 37.5.dp, 
        animationSpec = tween(durationMillis = 400),
        label = "play_pause_shape"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 🖼️ ALBUM ART - Now with clean transitions and matching width
        AnimatedContent(
            targetState = songId, // ✅ Transition per unique song
            transitionSpec = {
                (scaleIn(initialScale = 0.85f, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(400)))
                    .togetherWith(scaleOut(targetScale = 0.85f, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeOut(tween(400)))
            },
            label = "album_art_transition"
        ) { targetId ->
            // Gesture state is now isolated to the specific song instance
            val offsetX = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()

            Box(
                modifier = Modifier
                    .fillMaxWidth() // ✅ Width now matches the progress bar (Slider)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        translationX = offsetX.value
                        val scale = 1f - (abs(offsetX.value) / 1200f).coerceIn(0f, 0.2f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (abs(offsetX.value) / 800f).coerceIn(0f, 0.7f)
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value > 300) {
                                    scope.launch {
                                        offsetX.animateTo(1000f, tween(300))
                                        onPrev()
                                    }
                                } else if (offsetX.value < -300) {
                                    scope.launch {
                                        offsetX.animateTo(-1000f, tween(300))
                                        onNext()
                                    }
                                } else {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring())
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount)
                                }
                            }
                        )
                    }
                    .clip(RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model = albumArtUri ?: R.drawable.ic_music_placeholder,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ⏱️ PROGRESS BAR
        Slider(
            value = displayPosition.coerceIn(0f, if (duration > 0) duration.toFloat() else 1f),
            onValueChange = { 
                isDraggingSlider = true
                dragPosition = it 
            },
            onValueChangeFinished = {
                onSeek(dragPosition.toLong())
                isDraggingSlider = false
            },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
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
            FrostedGlassIconButton(
                icon = Icons.Default.Refresh,
                onClick = onRepeat,
                size = 48.dp,
                iconSize = 24.dp,
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) accentColor else Color.White
            )

            FrostedGlassIconButton(
                icon = Icons.Default.SkipPrevious,
                onClick = onPrev,
                size = 56.dp,
                iconSize = 32.dp
            )

            Surface(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(75.dp)
                    .clip(RoundedCornerShape(cornerRadius)),
                shape = RoundedCornerShape(cornerRadius), 
                color = accentColor 
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

            FrostedGlassIconButton(
                icon = Icons.Default.SkipNext,
                onClick = onNext,
                size = 56.dp,
                iconSize = 32.dp
            )

            FrostedGlassIconButton(
                icon = Icons.Default.Shuffle,
                onClick = onShuffle,
                size = 48.dp,
                iconSize = 24.dp,
                tint = if (isShuffleEnabled) accentColor else Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FrostedGlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Color.White
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
