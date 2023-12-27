package com.example.spotifyclone.exoplayer

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.spotifyclone.MainActivity
import com.example.spotifyclone.R
import com.example.spotifyclone.exoplayer.callbacks.MusicPlaybackPreparer
import com.example.spotifyclone.exoplayer.callbacks.MusicPlayerEventListener
import com.example.spotifyclone.exoplayer.callbacks.MusicPlayerNotificationListener
import com.example.spotifyclone.other.Constants.MEDIA_ROOT_ID
import com.example.spotifyclone.other.Constants.NETWORK_ERROR
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
//import com.plcoding.spotifycloneyt.exoplayer.MusicNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * /// NOTEEE
 * // WE have to register our music service in our manifest
 */

private const val SERVICE_TAG = "MusicService"
@AndroidEntryPoint
/**
Note that this will not inherit from the Normal service as expected but it will be used on another type of service.
 This service is also used as a foreground service
 // We also have to implement its functions
*/

// Note a song itself is a type of MediaMetadataCompat
class MusicService(
) : MediaBrowserServiceCompat(){

    @Inject
    lateinit var dataSourceFactory:DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer : SimpleExoPlayer

    // Then to inject the firebaseBase music source
    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    lateinit var activity: MainActivity

    private lateinit var musicNotificationManager: MusicNotificationManager

    // Since we do not want our service to interrupt the UI in the main thread we need to make use of coroutines
    private var serviceJob = Job()

    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob) // This is a way of creating our own custom coroutine scope so it does not run on the main alone

    /**
     * The purpose of mediaSession is likely to hold a reference to a MediaSessionCompat instance.
     * This media session can be used for tasks related to media playback, such as handling media controls,
     * providing metadata for media content, managing playback state, and interacting with media controllers.
     */
    private lateinit var mediaSession : MediaSessionCompat

    private lateinit var mediaSessionConnector : MediaSessionConnector  // This is used to connect with the media session

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat?=null

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    // Then to be able to track the duration of the song that is been played
    /**
     * companion object: The companion object is a way to define static members for a parent class.
     *  It's similar to static members in other languages. // Note
     */
    companion object{
        var curSongDuration = 0L
            private set // Note that private set means that this can be read from outside the service but can only be changed from within the service
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        /**
         * getLaunchIntentForPackage(packageName): This is a method provided by the packageManager.
         * It attempts to get an intent that, when launched, will open the main activity of the specified package.
         * It returns null if the package is not found or if it doesn't have a launch intent.
         */

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let{
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_MUTABLE)
        }
        activity = MainActivity()


        mediaSession = MediaSessionCompat(this,SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }
        // Then the main class "MediaBrowserServiceCompat()" needs a token
        sessionToken  = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,

             mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){
            curSongDuration  = exoPlayer.duration // this is to set the duration based on the duration of the exoplayer
        }
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong = it // This is the usefulness of the lambda function from the MusicPlaybackPreparer class
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true
            )
        }
        // Note that the mediaSessionConnector is used to connect the media session and the player
        mediaSessionConnector = MediaSessionConnector(mediaSession)

        // Then to use the connector for the play back
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)

        // Then to set the Queue Navigator for the media session connector
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())

        // Then to set the Player of the musicSessionConnector
        mediaSessionConnector.setPlayer(exoPlayer)

        // Then to add a Listener to the exoplayer (We also need to make this public)
        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)


        //val notificationPermissionIntent = Intent(this, MainActivity::class.java)

        // To request the permission

        Log.d("MainActivity", "getIT1: ${activity.getIT1}")

        if(true){

            musicNotificationManager.showNotification(exoPlayer)

        }
        // Then to show the notification

    }

    // This a way to always update the notification with a change in the song that is being played
    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description // Note that the window index is the index of the current song that is being played
        }
    }

    private fun preparePlayer(
        songs:List<MediaMetadataCompat>,
        itemToPlay:MediaMetadataCompat?,
        playNow :Boolean
    ){
        // To get the index of the song in our play list
        // If there is no current song playing then we wan to set the index to 0 but if not, we want to set the index to that of the current playing song
        val curSongIndex = if(curPlayingSong == null) 0 else songs.indexOf(itemToPlay)

        serviceScope.launch {
            exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory)) // Note that the media source is like a list of music that is useful if we want to concatenate through
            exoPlayer.seekTo(
                curSongIndex,
                0L
            ) // This is when the exoplayer iterates through and goes to the current song being played
            exoPlayer.playWhenReady = playNow // This is used to control the play
        }

    }

    /**
     * This function is used to keep track if the task of this service has been removed or not
     * This task is referring to the intent of the service
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Then to stop the exoplayer
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        // Then to remove the listener from the exoplayer once the service is destroyed
        exoPlayer.removeListener(musicPlayerEventListener)

        // When the service is destroyed, we want to release the exoplayer
        exoPlayer.release()
    }




    // Note that since different user will have different playlist, we want to avoid the intersection of the playlists
    // This root Id is used to make it unique
    // Note that if we want a kind of verification for the client then we can do that in the on get root
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID,null)
    }

    /**
     * This is where the loaded playlist of the client goes to
     * Note that the Media Item can be a PlayList, song or even an album itself
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        // Then to check if the parent id is equal to the media root id
        when(parentId) {
            MEDIA_ROOT_ID -> {
                // Note that resultsSent will either hold true or false based on if the result has been sent or not
                // This is also a matter of the status of the state based on the State data class; we can find this is in the FirebaseMusicSource class
                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
                    if(isInitialized){
                        // Then to set the result back to the client
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        // To check if it is not initialized and the list of songs is not empty
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()){
                            preparePlayer(firebaseMusicSource.songs, firebaseMusicSource.songs[0], false)
                            isPlayerInitialized = true; // set to true
                        }

                    }
                    // That is when the fire base music source is not ready
                    else{
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }

                }
                if(!resultsSent){
                    result.detach()
                }
            }
        }
    }
}