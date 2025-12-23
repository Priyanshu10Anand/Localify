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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
                            LocalJamsHeader()
                            FilterChipsSection()

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                itemsIndexed(librarySongs.take(3)) { _, song ->
                                    SongCard(
                                        song = song,
                                        isSelected = currentSong?.uri == song.uri,
                                        onClick = { viewModel.onSongTapped(song) }
                                    )
                                }

                                item {
                                    RecentlyPlayedSection(librarySongs)
                                }

                                item {
                                    Text(
                                        "Your Playlists",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                
                                items(librarySongs.drop(3)) { song ->
                                    SongCard(
                                        song = song,
                                        isSelected = currentSong?.uri == song.uri,
                                        onClick = { viewModel.onSongTapped(song) }
                                    )
                                }
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
                                ReplicatedMiniPlayer(
                                    song = song,
                                    isPlaying = isPlaying,
                                    progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onNext = { viewModel.next() },
                                    onRepeat = { viewModel.toggleRepeatMode() },
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
fun LocalJamsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Local Jams",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Box {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.Gray
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Surface(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd),
                shape = CircleShape,
                color = Color(0xFFF08E5B)
            ) {}
        }
    }
}

@Composable
fun FilterChipsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(text = "Songs", color = Color(0xFFF08E5B))
        FilterChip(text = "Playlists", color = Color(0xFF4A90E2))
        Text(
            "Albums",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FilterChip(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SongCard(song: Song, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = if (isSelected) Color(0xFF1DB954) else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun RecentlyPlayedSection(songs: List<Song>) {
    Column {
        Text(
            "Recently Played",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(songs.take(5)) { song ->
                RecentlyPlayedItem(song)
            }
        }
    }
}

@Composable
fun RecentlyPlayedItem(song: Song) {
    Column(modifier = Modifier.width(100.dp)) {
        AsyncImage(
            model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            song.title,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
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
}

@Composable
fun ReplicatedMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color(0xFF4A90E2))
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri ?: R.drawable.ic_music_placeholder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        song.artist,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onRepeat) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF282828), Color(0xFF121212))
                )
            )
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Text("▼", color = Color.White, fontSize = 20.sp)
                }
                Text(
                    "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onQueue) {
                    Text("≡", color = Color.White, fontSize = 24.sp)
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
