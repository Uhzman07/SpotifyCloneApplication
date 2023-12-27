package com.example.spotifyclone.exoplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
//import com.example.spotifyclone.Manifest
import android.Manifest
import android.app.Activity
import android.app.job.JobInfo.PRIORITY_DEFAULT
import android.content.Context.NOTIFICATION_SERVICE
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService

import com.example.spotifyclone.R
import com.example.spotifyclone.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.spotifyclone.other.Constants.NOTIFICATION_ID
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
class MusicNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit
) {

    private val notificationManager: PlayerNotificationManager

    init {

        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .apply {
                setChannelNameResourceId(R.string.notification_channel_name)
                setChannelDescriptionResourceId(R.string.notification_channel_description)
                setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
                setNotificationListener(notificationListener)
                setChannelImportance(NotificationManager.IMPORTANCE_HIGH)
                setSmallIconResourceId(R.drawable.ic_music)
                //setSmallIcon(R.drawable.ic_music)
                //setMediaSessionToken(sessionToken)

            }
            .build()

         notificationManager.setMediaSessionToken(sessionToken) // This makes the notification to take more space
         notificationManager.setPriority(NotificationCompat.PRIORITY_HIGH)




        //notificationManager.setMediaSessionToken(sessionToken)
        // notificationManager.setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.setSmallIcon(R.drawable.ic_music)
        //notificationManager.setColor(R.color.black)




    }

    fun showNotification(player: Player) {
        Log.d("Notification", "Show2")
        //notificationManager.setSmallIcon(R.drawable.ic_music)
        notificationManager.setPlayer(player)


    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback() // This ought to be updated anytime that our current song changes
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return mediaController.metadata.description.subtitle.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            Glide.with(context).asBitmap()
                .load(mediaController.metadata.description.iconUri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        callback.onBitmap(resource)
                        Log.d("MusicNotificationManager", "Large icon loaded successfully.")
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.d("MusicNotificationManager", "Large icon not loaded successfully.")
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                })
            return null
        }

    }
}