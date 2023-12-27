package com.example.spotifyclone.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.exoplayer.MusicServiceConnection
import com.example.spotifyclone.exoplayer.isPlayEnabled
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.isPrepared
import com.example.spotifyclone.other.Constants.MEDIA_ROOT_ID
import com.example.spotifyclone.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
):ViewModel(){
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState

    // Note that this is relative to when this class has been created effectively
    init{
        Log.d("Check1","Usman")
        _mediaItems.postValue(Resource.loading(null))
        Log.d("Check1",musicServiceConnection.isConnected.value.toString())
        /**
        if(musicServiceConnection.isConnected.value.toString() == "false"){
            Log.d("Check1","Usman2")
        }
        else{
            Log.d("Check1","UsmanZ")
        }
        */
        // Since we have only one list of songs then we can just make use of one Media ID
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                Log.d("Check2",children.size.toString())
                // Then to iterate through the media Items
                // This mapping is used to transform each media Item into an object
                val items = children.map{
                    Log.d("Check",it.description.title.toString())
                    Song(
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
                    )

                }
                _mediaItems.postValue(Resource.success(items))
            }

        })
    }

    /**
     * This functions makes use of the transport controls to control the songs
     */

    fun skipToNextSong(){
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong (){
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos:Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }

    /**
     * Note that this function will be used to play a new song or pause a song that is currently being played
     * Note that when toggle is set to "false", it plays the new song, and when it is set to "true" that means it has to pause the current song
     */
    fun playOrToggleSong(mediaItem:Song, toggle:Boolean = false){
        val isPrepared = playbackState.value?.isPrepared ?:false // This is set to false by default
        if(isPrepared && mediaItem.mediaId == curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)){
            playbackState.value?.let { playbackState ->
                when{
                    playbackState.isPlaying -> if(toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        }else{ // This is to check if it is not playing a song currently
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }

    // This is when the view Model is not used anymore
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID,object: MediaBrowserCompat.SubscriptionCallback(){})
    }



}