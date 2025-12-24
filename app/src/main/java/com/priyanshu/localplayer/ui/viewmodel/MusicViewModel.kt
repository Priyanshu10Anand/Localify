package com.priyanshu.localplayer.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.priyanshu.localplayer.data.model.Song
import com.priyanshu.localplayer.data.repository.MusicRepository
import com.priyanshu.localplayer.player.MusicService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val repository = MusicRepository(application)

    private val _librarySongs = MutableStateFlow<List<Song>>(emptyList())
    val librarySongs: StateFlow<List<Song>> = _librarySongs

    // 🔍 Search Logic with Debounce for smoothness
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(FlowPreview::class)
    val filteredSongs: StateFlow<List<Song>> = _searchQuery
        .debounce(200)
        .combine(_librarySongs) { query, songs ->
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
        // Collect songs from DB flow - This is the "Instant Loading" part
        viewModelScope.launch {
            repository.getSongsFlow().collect { songs ->
                _librarySongs.value = songs
                if (_queue.value.isEmpty()) {
                    _queue.value = songs
                    setMediaControllerItems(songs)
                }
            }
        }

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

                // Load initial songs if library is already fetched from DB
                if (_librarySongs.value.isNotEmpty()) {
                    setMediaControllerItems(_librarySongs.value)
                }

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
                _duration.value = song.duration
                _position.value = 0L 
                return
            }
        }
        
        if (mediaController?.currentMediaItem == null) {
            _currentSong.value = null
            _currentIndex.value = -1
            _duration.value = 0L
        }
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    if (it.isPlaying) {
                        _position.value = it.currentPosition
                        val dur = it.duration
                        if (dur > 0 && dur != C.TIME_UNSET) {
                            _duration.value = dur
                        }
                    }
                }
                delay(500)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            // This now triggers the background sync
            repository.refreshLibrary()
        }
    }

    private fun setMediaControllerItems(songs: List<Song>) {
        val controller = mediaController ?: return
        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri)
                .build()

            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .setMediaMetadata(metadata)
                .build()
        }
        controller.setMediaItems(mediaItems)
        controller.prepare()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSongTapped(song: Song) {
        val controller = mediaController ?: return
        
        val currentQueue = _queue.value
        val indexInQueue = currentQueue.indexOf(song)
        
        if (indexInQueue != -1 && controller.mediaItemCount == currentQueue.size) {
            controller.seekTo(indexInQueue, 0)
            controller.play()
        } else {
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
            playFromQueue(songs.indexOf(song))
        }
    }

    fun playFromQueue(index: Int) {
        val songs = _queue.value
        if (index !in songs.indices) return
        val controller = mediaController ?: return

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems, index, 0)
        controller.prepare()
        controller.play()

        _currentIndex.value = index
        _currentSong.value = songs[index]
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
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

    fun shuffleAndPlay() {
        val controller = mediaController ?: return
        val library = _librarySongs.value
        if (library.isEmpty()) return

        val shuffledList = library.shuffled()
        _queue.value = shuffledList
        _isShuffleEnabled.value = true
        
        val mediaItems = shuffledList.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .build()
                )
                .build()
        }
        
        controller.setMediaItems(mediaItems)
        controller.prepare()
        controller.play()
        
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

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
