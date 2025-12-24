package com.priyanshu.localplayer.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumArtUriString: String? // Store as String for Room
) {
    val albumArtUri: Uri?
        get() = albumArtUriString?.let { Uri.parse(it) }
}
