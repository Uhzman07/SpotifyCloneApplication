package com.example.spotifyclone.exoplayer

//import android.bluetooth.BluetoothA2dp.STATE_PLAYING
import android.media.metrics.PlaybackStateEvent.STATE_PAUSED
import android.media.metrics.PlaybackStateEvent.STATE_PLAYING
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

// Note that the "inline" is tailored for the get()
/**
 * the PlaybackStateCompat class.
 * Extension properties allow you to add new properties to existing classes without modifying their code.
 * Note that "state" is also used to specify the state of the PlaybackStateCompat
 */

/**
 * Really Important Warning
 * Make sure not to use the import of android.bluetooth.BluetoothA2dp.STATE_PLAYING
 * Use android.media.metrics.PlaybackStateEvent.STATE_PLAYING so as to get a seamless update
 */


inline val PlaybackStateCompat.isPrepared
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state ==  PlaybackStateCompat.STATE_PLAYING ||
            state == PlaybackStateCompat.STATE_PAUSED


inline val PlaybackStateCompat.isPlaying
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state ==  PlaybackStateCompat.STATE_PLAYING


inline val PlaybackStateCompat.isPlayEnabled
    // Since we are making use of binary flags here, we make use of the "and"
    get() = actions and PlaybackStateCompat.ACTION_PLAY != 0L ||
            (actions and PlaybackStateCompat.ACTION_PAUSE != 0L && // This is to check if it is already paused or if we are in the paused state
                    state == PlaybackStateCompat.STATE_PAUSED)

// This is to get the position of the song
inline val PlaybackStateCompat.currentPlaybackPosition : Long
    get() {
        Log.d("StateChecker", "Current State: $state")
        return if (state == STATE_PLAYING) { // Playing
            Log.d("StateChecker", "Working State ${state.toString()}")
            // The time of the song is calculate by subtracting the time of the last update from the system time which it was last booted
            // for "lastPositionUpdateTime"
            // Get the elapsed real time at which position was last updated. If the position has never been set this will return 0;
            val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
            // Then for what we will return
            // Note that we get the "position" here from the PlaybackStateCompat which is used to signify the position of the current song
            // We also need to consider the speed of the change so as to match up with the real position change
            (position + (timeDelta * playbackSpeed)).toLong()

        } else {
            Log.d("StateChecker", "Outside STATE_PLAYING block. State: $state, Position: $position")
            position
        }
    }


