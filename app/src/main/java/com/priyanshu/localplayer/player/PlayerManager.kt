package com.priyanshu.localplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(context: Context) {

    private val player = ExoPlayer.Builder(context).build()

    private var currentUri: String? = null

    fun playOrToggle(uri: String) {
        if (currentUri == uri) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        } else {
            currentUri = uri
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
        }
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun getCurrentUri(): String? = currentUri

    fun release() {
        player.release()
    }
}
