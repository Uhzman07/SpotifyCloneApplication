package com.example.spotifyclone.exoplayer.callbacks

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.spotifyclone.exoplayer.MusicService
import com.example.spotifyclone.other.Constants.NOTIFICATION_ID
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener{
    // This is when the user has just like swiped the notification away or something
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            stopForeground(true)
            isForegroundService = false
            stopSelf() // This will itself stop the service

        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        // Works
        musicService.apply {
            if(ongoing && !isForegroundService){ // Note that ongoing is used to check if the service itself is active
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext,this::class.java) // "this here refers to the music service
                    //  Note that "this" intent is used to also start the class
                )
                startForeground(NOTIFICATION_ID,notification) // Note that the foreground is the actual notification
                isForegroundService = true
            }
        }
    }
}