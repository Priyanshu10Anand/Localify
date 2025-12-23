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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
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

                    val librarySongs by viewModel.librarySongs.collectAsState()
                    val queue by viewModel.queue.collectAsState()
                    val currentSong by viewModel.currentSong.collectAsState()
                    val currentIndex by viewModel.currentIndex.collectAsState()
                    val isPlaying by viewModel.isPlaying.collectAsState()
                    val position by viewModel.position.collectAsState()
                    val duration by viewModel.duration.collectAsState()
                    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
                    val repeatMode by viewModel.repeatMode.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            LocalifyHeader()
                            MusicTabs()

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                itemsIndexed(librarySongs) { _, song ->
                                    SimpleSongItem(
                                        song = song,
                                        isSelected = currentSong?.uri == song.uri,
                                        onClick = { viewModel.onSongTapped(song) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 24.dp, bottom = 110.dp)
                        ) {
                            FloatingActionButton(
                                onClick = { viewModel.toggleShuffle() },
                                containerColor = Color(0xFF5E67A2),
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                            }
                        }

                        AnimatedVisibility(
                            visible = currentSong != null,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        ) {
                            currentSong?.let { song ->
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
fun LocalifyHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Localify",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Row {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun MusicTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Songs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color(0xFF5E67A2)))
        }
        Text("Albums", color = Color.Gray, fontSize = 16.sp)
        Text("Artists", color = Color.Gray, fontSize = 16.sp)
        Text("Genres", color = Color.Gray, fontSize = 16.sp)
        Text("Playlists", color = Color.Gray, fontSize = 16.sp)
    }
}

@Composable
fun SimpleSongItem(song: Song, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isSelected) Color(0xFF5E67A2) else Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
        }
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
            .fillMaxWidth()
            .height(75.dp)
            .clickable { onClick() },
        color = Color(0xFF121212)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.2f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color(0xFF5E67A2)))
            }
            
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
                    contentDescription = null,
                    modifier = Modifier.size(45.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
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
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
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
