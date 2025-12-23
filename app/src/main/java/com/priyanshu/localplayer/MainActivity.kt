package com.priyanshu.localplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.priyanshu.localplayer.data.model.Song
import com.priyanshu.localplayer.ui.components.NowPlayingPolished
import com.priyanshu.localplayer.ui.components.QueueSheet
import com.priyanshu.localplayer.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.loadSongs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF1DB954),
                    background = Color.Black,
                    surface = Color(0xFF121212)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    LaunchedEffect(Unit) { requestAudioPermission() }

                    var showNowPlaying by remember { mutableStateOf(false) }
                    var showQueue by remember { mutableStateOf(false) }

                    var isSearchActive by remember { mutableStateOf(false) }
                    val searchQuery by viewModel.searchQuery.collectAsState()

                    val filteredSongs by viewModel.filteredSongs.collectAsState()
                    val queue by viewModel.queue.collectAsState()
                    val currentSong by viewModel.currentSong.collectAsState()
                    val currentIndex by viewModel.currentIndex.collectAsState()
                    val isPlaying by viewModel.isPlaying.collectAsState()
                    val position by viewModel.position.collectAsState()
                    val duration by viewModel.duration.collectAsState()
                    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
                    val repeatMode by viewModel.repeatMode.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // 🖼️ Thumbnail Grid - pused down by safe column to avoid bleeding
                        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(
                                    start = 16.dp, 
                                    end = 16.dp, 
                                    top = 80.dp, 
                                    bottom = 180.dp 
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredSongs) { song ->
                                    ThumbnailSongItem(
                                        song = song,
                                        isSelected = currentSong?.uri == song.uri,
                                        onClick = { viewModel.onSongTapped(song) }
                                    )
                                }
                            }
                        }

                        // 🧊 FLOATING TOP BUTTONS & HEADER GRADIENT
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            // ✅ Top Black Gradient (same smooth implementation as bottom)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black,
                                                Color.Black.copy(alpha = 0.8f),
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Localify "Button"
                                AnimatedVisibility(
                                    visible = !isSearchActive,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    GlassButton(text = "Localify")
                                }

                                // Right: Search Area
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    if (isSearchActive) {
                                        GlassSearchField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                                            onClose = { 
                                                isSearchActive = false
                                                viewModel.onSearchQueryChanged("")
                                            }
                                        )
                                    } else {
                                        GlassIconButton(
                                            icon = Icons.Default.Search,
                                            onClick = { isSearchActive = true }
                                        )
                                    }
                                }
                            }
                        }

                        // ⬛ BOTTOM GRADIENT OVERLAY
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.8f),
                                            Color.Black
                                        )
                                    )
                                )
                        )

                        // 🔀 Floating Shuffle Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 100.dp)
                                .wrapContentSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .blur(25.dp)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )

                            Surface(
                                onClick = { viewModel.shuffleAndPlay() },
                                color = Color.Transparent, 
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.4f)), 
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "SHUFFLE PLAY", 
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold, 
                                        letterSpacing = 1.2.sp,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // 🎶 Floating Mini Player
                        AnimatedVisibility(
                            visible = currentSong != null,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        ) {
                            currentSong?.let { song ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                        .fillMaxWidth()
                                        .height(64.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .blur(25.dp)
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )

                                    SimpleBottomPlayer(
                                        song = song,
                                        isPlaying = isPlaying,
                                        progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                                        onPlayPause = { viewModel.togglePlayPause() },
                                        onNext = { viewModel.next() },
                                        onClick = { showNowPlaying = true }
                                    )
                                }
                            }
                        }
                    }

                    if (showNowPlaying && currentSong != null) {
                        FullPlayerDialog(
                            song = currentSong!!,
                            position = position,
                            duration = duration,
                            isPlaying = isPlaying,
                            isShuffleEnabled = isShuffleEnabled,
                            repeatMode = repeatMode,
                            viewModel = viewModel,
                            onDismiss = { showNowPlaying = false },
                            onQueue = { showQueue = true }
                        )
                    }

                    if (showQueue && queue.isNotEmpty()) {
                        QueueSheet(
                            songs = queue,
                            currentIndex = currentIndex,
                            onSongSelected = { index ->
                                viewModel.playFromQueue(index)
                                showQueue = false
                            },
                            onDismiss = { showQueue = false }
                        )
                    }
                }
            }
        }
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }
    }
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(50.dp).wrapContentWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(25.dp)) 
                .blur(20.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxHeight().wrapContentWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun GlassIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.size(50.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape) 
                .blur(20.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Surface(
            color = Color.Transparent,
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(25.dp))
                .blur(20.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF1DB954),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                trailingIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun ThumbnailSongItem(song: Song, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        AsyncImage(
            model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = if (isSelected) Color(0xFF1DB954) else Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SimpleBottomPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        color = Color.Transparent, 
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        song.artist,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color.White,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun FullPlayerDialog(
    song: Song,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onQueue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Now playing", color = Color.Gray, fontSize = 12.sp)
                    Text("All songs", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                }
            }

            NowPlayingPolished(
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumArtUri = song.albumArtUri,
                position = position,
                duration = duration,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onSeek = { viewModel.seekTo(it) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrev = { viewModel.previous() },
                onShuffle = { viewModel.toggleShuffle() },
                onRepeat = { viewModel.toggleRepeatMode() },
                onQueue = onQueue
            )
        }
    }
}
