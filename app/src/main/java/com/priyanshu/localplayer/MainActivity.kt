package com.priyanshu.localplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.priyanshu.localplayer.data.model.Song
import com.priyanshu.localplayer.ui.components.NowPlayingPolished
import com.priyanshu.localplayer.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

                    var lastNonNullSong by remember { mutableStateOf<Song?>(null) }
                    LaunchedEffect(currentSong) {
                        if (currentSong != null) {
                            lastNonNullSong = currentSong
                        }
                    }

                    // 🎨 Dynamic Theming Logic
                    val context = LocalContext.current
                    var dominantColor by remember { mutableStateOf(Color(0xFF1DB954)) } // Default green

                    LaunchedEffect(currentSong) {
                        currentSong?.albumArtUri?.let { uri ->
                            val bitmap = fetchBitmap(context, uri.toString())
                            if (bitmap != null) {
                                withContext(Dispatchers.Default) {
                                    val palette = Palette.from(bitmap).generate()
                                    // Try to get a vibrant color, fallback to dominant
                                    val color = palette.getVibrantColor(palette.getDominantColor(0xFF1DB954.toInt()))
                                    dominantColor = Color(color)
                                }
                            }
                        }
                    }

                    BackHandler(enabled = showNowPlaying) {
                        showNowPlaying = false
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        
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
                                        accentColor = dominantColor,
                                        onClick = { viewModel.onSongTapped(song) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
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
                                AnimatedVisibility(visible = !isSearchActive) {
                                    GlassButton(text = "Localify")
                                }

                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    if (isSearchActive) {
                                        GlassSearchField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                                            accentColor = dominantColor,
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

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 110.dp)
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
                                        .height(80.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(16.dp))
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

                        AnimatedVisibility(
                            visible = showNowPlaying && lastNonNullSong != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            InteractivePlayerDialog(
                                song = currentSong ?: lastNonNullSong!!,
                                position = position,
                                duration = duration,
                                isPlaying = isPlaying,
                                isShuffleEnabled = isShuffleEnabled,
                                repeatMode = repeatMode,
                                queue = queue,
                                currentIndex = currentIndex,
                                accentColor = dominantColor,
                                viewModel = viewModel,
                                onDismiss = { showNowPlaying = false }
                            )
                        }
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

    private suspend fun fetchBitmap(context: Context, url: String): Bitmap? {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Required for Palette
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        return (result as? BitmapDrawable)?.bitmap
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractivePlayerDialog(
    song: Song,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    queue: List<Song>,
    currentIndex: Int,
    accentColor: Color,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 45.dp, // Corrected peek height to avoid clipping buttons
        sheetDragHandle = null,
        sheetContainerColor = Color.Transparent,
        containerColor = Color.Transparent,
        sheetContent = {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(1f)) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(80.dp),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))

                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        itemsIndexed(queue) { index, queueSong ->
                            val isCurrent = index == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playFromQueue(index) }
                                    .background(if (isCurrent) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 4.dp), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = queueSong.albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), 
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        queueSong.title,
                                        color = if (isCurrent) accentColor else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        queueSong.artist, 
                                        color = Color.LightGray, 
                                        fontSize = 13.sp, 
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = song.albumArtUri, animationSpec = tween(700)) { artUri ->
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)))

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Now playing", color = Color.Gray, fontSize = 12.sp)
                        Text("Localify Library", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(48.dp))
                }

                NowPlayingPolished(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    albumArtUri = song.albumArtUri,
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    accentColor = accentColor,
                    onSeek = { viewModel.seekTo(it) },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.next() },
                    onPrev = { viewModel.previous() },
                    onShuffle = { viewModel.toggleShuffle() },
                    onRepeat = { viewModel.toggleRepeatMode() },
                    onQueue = { scope.launch { scaffoldState.bottomSheetState.expand() } }
                )
            }
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
    accentColor: Color,
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
                    cursorColor = accentColor,
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
fun ThumbnailSongItem(song: Song, isSelected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        AsyncImage(
            model = song.albumArtUri,
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
            color = if (isSelected) accentColor else Color.White,
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
        shape = RoundedCornerShape(16.dp),
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
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        song.artist,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
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
