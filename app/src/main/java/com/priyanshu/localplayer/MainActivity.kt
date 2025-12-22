package com.priyanshu.localplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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

        setContent {

            LaunchedEffect(Unit) { requestAudioPermission() }

            var showQueue by remember { mutableStateOf(false) }

            val songs by viewModel.songs.collectAsState()
            val currentSong by viewModel.currentSong.collectAsState()
            val isPlaying by viewModel.isPlaying.collectAsState()
            val position by viewModel.position.collectAsState()
            val duration by viewModel.duration.collectAsState()
            val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
            val repeatMode by viewModel.repeatMode.collectAsState()

            Column {

                // 🎵 SONG LIST
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(songs) { song ->
                        Text(
                            text = song.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable { viewModel.onSongTapped(song) }
                        )
                    }
                }

                // 🎶 NOW PLAYING
                currentSong?.let { song ->
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
                        onSeek = viewModel::seekTo,
                        onPlayPause = viewModel::togglePlayPause,
                        onNext = viewModel::next,
                        onPrev = viewModel::previous,
                        onShuffle = viewModel::toggleShuffle,
                        onRepeat = viewModel::toggleRepeatMode,
                        onQueue = { showQueue = true }
                    )
                }
            }

            // 📜 QUEUE SHEET
            if (showQueue) {
                QueueSheet(
                    songs = songs,
                    currentIndex = songs.indexOf(currentSong),
                    onSongSelected = { index ->
                        viewModel.onSongTapped(songs[index])
                        showQueue = false
                    },
                    onDismiss = { showQueue = false }
                )
            }
        }
    }

    private fun requestAudioPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }
    }
}
