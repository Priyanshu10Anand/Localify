package com.priyanshu.localplayer.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.priyanshu.localplayer.data.model.Song
import com.priyanshu.localplayer.data.repository.MusicRepository
import com.priyanshu.localplayer.player.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val currentIndexState: Int get() = currentIndex
    private val app = application
    private val repository = MusicRepository(application)

    private var songQueue: List<Song> = emptyList()
    private var currentIndex = -1

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    // 🔀 SHUFFLE
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    // 🔁 REPEAT
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private var mediaController: MediaController? = null

    init {
        val sessionToken = SessionToken(
            app,
            ComponentName(app, MusicService::class.java)
        )

        val controllerFuture =
            MediaController.Builder(app, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()

                mediaController?.addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int
                    ) {
                        mediaItem?.mediaId?.toIntOrNull()?.let {
                            currentIndex = it
                            _currentSong.value = songQueue.getOrNull(it)
                        }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _isShuffleEnabled.value = shuffleModeEnabled
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }
                })

                startPositionUpdates()
            },
            Runnable::run
        )
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    _position.value = it.currentPosition
                    _duration.value = if (it.duration > 0) it.duration else 0L
                }
                delay(500)
            }
        }
    }

    fun loadSongs() {
        songQueue = repository.getAllSongs()
        _songs.value = songQueue
    }

    fun onSongTapped(song: Song) {
        currentIndex = songQueue.indexOf(song)
        if (currentIndex == -1) return

        playAtIndex(currentIndex)
    }

    private fun playAtIndex(index: Int) {
        val mediaItems = songQueue.mapIndexed { i, song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(i.toString())
                .build()
        }

        _currentSong.value = songQueue[index]

        mediaController?.setMediaItems(mediaItems, index, 0)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun next() {
        mediaController?.seekToNext()
    }

    fun previous() {
        mediaController?.seekToPrevious()
    }

    // 🔀 SHUFFLE TOGGLE
    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    // 🔁 REPEAT TOGGLE
    fun toggleRepeatMode() {
        mediaController?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }
}
