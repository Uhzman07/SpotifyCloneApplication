package com.example.spotifyclone.exoplayer

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.provider.MediaStore.Video.Media
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.MediaController
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.spotifyclone.other.Constants.NETWORK_ERROR
import com.example.spotifyclone.other.Event
import com.example.spotifyclone.other.Resource

// The aim of this class is to integrate the Event class and the Resource class for the purpose of error handling
// This class is expected communicate between the view model and the activity
class MusicServiceConnection (
    context: Context
    ){

    // We want this to be be only alterable from within this class
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    // This is the one that we want to be open to all
    val isConnected : LiveData<Event<Resource<Boolean>>> = _isConnected

    // This is to check if there is a network error
    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError : LiveData<Event<Resource<Boolean>>> = _networkError

    // Then to check a music is been played currently
    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState : LiveData<PlaybackStateCompat?> = _playbackState

    // To get the data about the currently playing song
    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val curPlayingSong : LiveData<MediaMetadataCompat?> = _curPlayingSong


    // This is used to control the song
    lateinit var mediaController: MediaControllerCompat

    // The to create the instance of our inner class which is the media connection call back class
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    // To create the media browser for this
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() } // Note that we have to add this connect

    // Then to set the media controller
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls // Note that the get() function here is used to wait on the mediaController to prevent crashing just in case it is not loaded yet.

    /**
     * In Android, the MediaBrowserCompat class is used to connect to a media browser service,
     * such as a media playback service, and interact with the media content provided by that service.
     * The subscribe method is used to subscribe to changes in the content hierarchy,
     * typically changes in the structure of the media content (like playlists, albums, etc.).
     */
    fun subscribe(parentId:String, callback: MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.unsubscribe(parentId,callback)

    }


    /**
     * Note that this class is used for the direct connection between the Browser and its connection
     */
    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ): MediaBrowserCompat.ConnectionCallback(){
        // This is when it is connected
        override fun onConnected() {
            //Log.d("Check1", "Connected")
            // Then to create our mediaController
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                // This is to add the media controller call back to it
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))

            Log.d("Check1", isConnected.value.toString())
            Log.d("Check1", isConnected.value.toString())


        }


        override fun onConnectionSuspended() {
            //super.onConnectionSuspended()
            _isConnected.postValue(Event(Resource.error(
                "The connection was suspended", false
            )
            ))
        }

        override fun onConnectionFailed() {
            //super.onConnectionFailed()
            _isConnected.postValue(Event(Resource.error(
                "Couldn't connect to media browser",
                false
            )
            ))
        }

    }


    /**
     * This class entails all the functions that are related to the controller
     */
    private inner class MediaControllerCallback: MediaControllerCompat.Callback(){

        // Whenever our playback state changes
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            //super.onPlaybackStateChanged(state)
            // Then to post the value of the states
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            //super.onMetadataChanged(metadata)
            _curPlayingSong.postValue(metadata)
        }


        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when(event){
                // Then to post an error resource when we are given an error event in
                // Note that this NETWORK_ERROR is sent from the service class, it is caught here and then it is worked upon.
                // It is represented as mediaSession.sendSessionEvent() in under the onLoadChildren function in the music Service class
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your intenet connection.",
                            null
                        )
                    )
                )
            }
        }

        // Just in case our session is destroyed at one point
        override fun onSessionDestroyed() {
            //super.onSessionDestroyed()
            mediaBrowserConnectionCallback.onConnectionSuspended() // Then to call the function
        }



    }







}