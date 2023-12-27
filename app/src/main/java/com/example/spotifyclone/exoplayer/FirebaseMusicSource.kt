package com.example.spotifyclone.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import android.util.Log
import androidx.core.net.toUri
import com.example.spotifyclone.data.remote.MusicDatabase
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.PriorityDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("DEPRECATION")
class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
){

    var songs = emptyList<MediaMetadataCompat>() // This will contain meta information about the song

    //val checkAllSongs = musicDatabase.getAllSongs()

    suspend fun fetchMediaData() = withContext(Dispatchers.IO){
        state = State.STATE_INITIALIZING
        val allSongs = musicDatabase.getAllSongs()
        Log.d("Checker", "This is the size of the media item: ${allSongs.size}")
        // To then transform the list of songs into media meta data so that it is also in form of the media metadata
        songs = allSongs.map { song ->
            Log.e("Checker1", song.mediaId)



            // To then fill the the MediaMetadataCompat with the info of our actual song
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST,song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.subtitle)
                .build()
        }
        state = State.STATE_INITIALIZED
    }

    // NOTE : The concatenating music source
    // This is an object that concatenates multiple media sources into a single source.
    // It's part of the ExoPlayer library and is useful when you want to play a sequence of media items seamlessly.
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory) : ConcatenatingMediaSource{
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaItem = MediaItem.Builder()
                .setUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
                .build()
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return  concatenatingMediaSource
    }

    // Note that is returns a normal List automatically
    fun asMediaItems() = songs.map{ song ->
        val mediaId = song.getString(METADATA_KEY_MEDIA_ID)
        Log.e("Checker", mediaId)
        if (mediaId.isNullOrEmpty()) {
            // Log an error or handle the case where the MediaId is empty or null.
            Log.e("Checker", "MediaId is null or empty for song: ${song.getString(METADATA_KEY_TITLE)}")
        }
        else{
            Log.e("Checker","MediaId is not null or empty for song: ${song.getString(METADATA_KEY_TITLE)}")
        }

        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.getString(METADATA_KEY_MEDIA_ID))
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList() // This is used to change to a mutable list instead

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = State.STATE_CREATED
        /**
         *  The set block is used to monitor changes to the state property and notify listeners
         *  when it reaches the STATE_INITIALIZED or STATE_ERROR state.
         */
        set(value){
            if(value == State.STATE_INITIALIZED || value == State.STATE_ERROR){
                // Note that synchronized is used to make sure that it runs on one thread only
                // Synchronization is used to ensure that multiple threads don't update the state at the same time.
                synchronized(onReadyListeners){
                    //The field identifier is used within the set accessor to refer to the backing field where the new value should be stored.
                    field = value // Note that field here will represent the current State and value is the new value
                    onReadyListeners.forEach{ listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }

            }
            else{
                field = value
            }
        }

    fun whenReady(action : (Boolean) -> Unit) : Boolean{
        if(state == State.STATE_CREATED || state == State.STATE_INITIALIZING){
            onReadyListeners += action
            return false
        }else{
            action(state == State.STATE_INITIALIZED)
            return true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}