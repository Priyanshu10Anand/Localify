package com.priyanshu.localplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player)
            .setId("LocalPlayerSession")
            .build()

        createNotificationChannel()
        setupNotification()
    }

    private fun setupNotification() {
        notificationManager = PlayerNotificationManager.Builder(this, 1, "music_playback")
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return player.mediaMetadata.title ?: "LocalPlayer"
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return player.mediaMetadata.artist
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? = null

                override fun createCurrentContentIntent(player: Player): PendingIntent? = null
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            })
            .build().apply {
                setPlayer(player)
                setUsePlayPauseActions(true)
                setUseNextAction(true)
                setUsePreviousAction(true)
            }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "music_playback",
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession = mediaSession

    override fun onDestroy() {
        notificationManager.setPlayer(null)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
