package com.priyanshu.localplayer.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.priyanshu.localplayer.data.model.Song
import com.priyanshu.localplayer.data.repository.MusicRepository
import com.priyanshu.localplayer.player.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val repository = MusicRepository(application)

    private val _librarySongs = MutableStateFlow<List<Song>>(emptyList())
    val librarySongs: StateFlow<List<Song>> = _librarySongs

    // 🔍 Search Logic
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredSongs: StateFlow<List<Song>> = combine(_librarySongs, _searchQuery) { songs, query ->
        if (query.isEmpty()) {
            songs
        } else {
            songs.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) 
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        _isPlaying.value = playWhenReady
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                            _isPlaying.value = false
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updatePlaybackState(mediaItem)
                    }

                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        updatePlaybackState(mediaController?.currentMediaItem)
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

    private fun updatePlaybackState(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId
        if (mediaId != null) {
            val song = _librarySongs.value.find { it.uri == mediaId }
            if (song != null) {
                _currentSong.value = song
                _currentIndex.value = _queue.value.indexOf(song)
                return
            }
        }
        
        if (mediaController?.currentMediaItem == null) {
            _currentSong.value = null
            _currentIndex.value = -1
        }
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
        viewModelScope.launch {
            val songs = repository.getAllSongs()
            _librarySongs.value = songs
            if (_queue.value.isEmpty()) {
                _queue.value = songs
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
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

        _isPlaying.value = true

        mediaController?.apply {
            setMediaItems(mediaItems, index, 0)
            prepare()
            playWhenReady = true 
            play()
        }

        _currentIndex.value = index
        _currentSong.value = songs[index]
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        val currentPlayState = _isPlaying.value
        _isPlaying.value = !currentPlayState
        if (currentPlayState) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun next() {
        _isPlaying.value = true
        mediaController?.apply {
            playWhenReady = true 
            seekToNext()
        }
    }

    fun previous() {
        _isPlaying.value = true
        mediaController?.apply {
            playWhenReady = true 
            seekToPrevious()
        }
    }

    fun shuffleAndPlay() {
        val controller = mediaController ?: return
        val library = _librarySongs.value
        if (library.isEmpty()) return

        val shuffledList = library.shuffled()
        _queue.value = shuffledList
        _isShuffleEnabled.value = true
        _isPlaying.value = true
        
        val mediaItems = shuffledList.map { song ->
            MediaItem.Builder().setUri(song.uri).setMediaId(song.uri).build()
        }
        
        controller.apply {
            setMediaItems(mediaItems)
            prepare()
            playWhenReady = true 
            play()
        }
        
        _currentIndex.value = 0
        _currentSong.value = shuffledList[0]
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val currentMediaItem = controller.currentMediaItem ?: return
        val currentMediaId = currentMediaItem.mediaId
        val currentSong = _librarySongs.value.find { it.uri == currentMediaId } ?: _currentSong.value ?: return
        
        val otherSongs = _librarySongs.value.filter { it.uri != currentSong.uri }.shuffled()
        val newQueue = listOf(currentSong) + otherSongs
        
        _queue.value = newQueue
        _currentIndex.value = 0
        _isShuffleEnabled.value = true

        val currentInPlayerIndex = controller.currentMediaItemIndex
        if (currentInPlayerIndex != 0) {
            controller.moveMediaItem(currentInPlayerIndex, 0)
        }
        
        if (controller.mediaItemCount > 1) {
            controller.removeMediaItems(1, controller.mediaItemCount)
        }
        
        val otherMediaItems = otherSongs.map { song ->
            MediaItem.Builder().setUri(song.uri).setMediaId(song.uri).build()
        }
        if (otherMediaItems.isNotEmpty()) {
            controller.addMediaItems(1, otherMediaItems)
        }
        
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
