package com.example.spotifyclone.exoplayer

import android.support.v4.media.MediaMetadataCompat
import com.example.spotifyclone.data.entities.Song

/**
 * This file contains a method that is used to convert the MediaMetadataCompat to a Song object
 */
fun MediaMetadataCompat.toSong(): Song? {
    return description?.let {
        Song(
            it.mediaId ?: "",
            it.title.toString(),
            it.subtitle.toString(),
            it.mediaUri.toString(),
            it.iconUri.toString()
        )
    }


}