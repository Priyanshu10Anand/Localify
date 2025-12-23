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

    private val app = application
    private val repository = MusicRepository(application)

    private val _librarySongs = MutableStateFlow<List<Song>>(emptyList())
    val librarySongs: StateFlow<List<Song>> = _librarySongs

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

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
                        val index = mediaController?.currentMediaItemIndex ?: -1
                        _currentIndex.value = index
                        _currentSong.value = _queue.value.getOrNull(index)
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        // Managed manually for UI synchronization
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
        val songs = repository.getAllSongs()
        _librarySongs.value = songs

        if (_queue.value.isEmpty()) {
            _queue.value = songs
        }
    }

    fun onSongTapped(song: Song) {
        val songs = if (_isShuffleEnabled.value) {
            val list = _librarySongs.value.toMutableList()
            list.remove(song)
            val shuffled = list.shuffled().toMutableList()
            shuffled.add(0, song)
            shuffled
        } else {
            _librarySongs.value
        }

        _queue.value = songs
        val index = songs.indexOf(song)
        playFromQueue(index)
    }

    fun playFromQueue(index: Int) {
        val songs = _queue.value
        if (index !in songs.indices) return

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .build()
        }

        mediaController?.setMediaItems(mediaItems, index, 0)
        mediaController?.prepare()
        mediaController?.play()

        _currentIndex.value = index
        _currentSong.value = songs[index]
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

    /**
     * Seamlessly reshuffles the queue.
     * Moves the current song to the start of the playlist and shuffles the rest,
     * updating the player without any audio interruption.
     */
    fun toggleShuffle() {
        val controller = mediaController ?: return
        
        // 1. Identify the current song safely
        val currentMediaItem = controller.currentMediaItem ?: return
        val currentMediaId = currentMediaItem.mediaId
        val currentSong = _librarySongs.value.find { it.uri == currentMediaId } ?: _currentSong.value ?: return
        
        // 2. Generate a new shuffled list starting with the current song
        val otherSongs = _librarySongs.value.filter { it.uri != currentSong.uri }.shuffled()
        val newQueue = listOf(currentSong) + otherSongs
        
        // Update local state for UI immediately
        _queue.value = newQueue
        _currentIndex.value = 0
        _isShuffleEnabled.value = true

        // 3. Seamlessly update the ExoPlayer queue
        val currentInPlayerIndex = controller.currentMediaItemIndex
        
        // Move current item to index 0 (this is a seamless operation)
        if (currentInPlayerIndex != 0) {
            controller.moveMediaItem(currentInPlayerIndex, 0)
        }
        
        // Remove everything except the current song at index 0
        if (controller.mediaItemCount > 1) {
            controller.removeMediaItems(1, controller.mediaItemCount)
        }
        
        // Add the rest of the shuffled items (this is also seamless)
        val otherMediaItems = otherSongs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .build()
        }
        if (otherMediaItems.isNotEmpty()) {
            controller.addMediaItems(1, otherMediaItems)
        }
        
        // Ensure ExoPlayer's internal shuffle is OFF since we are managing order manually
        controller.shuffleModeEnabled = false 
    }

    fun toggleRepeatMode() {
        mediaController?.let { controller ->
            val newMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = newMode
            _repeatMode.value = newMode
        }
    }
}
