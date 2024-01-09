package com.example.spotifyclone.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifyclone.exoplayer.MusicService
import com.example.spotifyclone.exoplayer.MusicServiceConnection
import com.example.spotifyclone.exoplayer.currentPlaybackPosition
import com.example.spotifyclone.other.Constants.UPDATE_PLAYER_POSITION_INTERVAL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SongViewModel @Inject constructor(
    musicServiceConnection: MusicServiceConnection

):ViewModel(){
    private val playbackState = musicServiceConnection.playbackState

    private val _curSongDuration = MutableLiveData<Long>()
    val curSongDuration:LiveData<Long> = _curSongDuration

    private val _curPlayerPosition= MutableLiveData<Long>()
    val curPlayerPosition:LiveData<Long> = _curPlayerPosition

    init {
        updateCurrentPlayerPosition()
    }

    // Note that we can use this while loop because the song view model is cleared once the song fragment is exited
    // And then we are making use of a coroutine scope

    private fun updateCurrentPlayerPosition(){





        /*
         while(true){
                val pos = playbackState.value?.currentPlaybackPosition

                if(curPlayerPosition.value != pos){

                    _curPlayerPosition.postValue(pos!!)
                    Log.d("Main Duration 2", pos.toString())
                    _curSongDuration.postValue(MusicService.curSongDuration)
                }
                delay(UPDATE_PLAYER_POSITION_INTERVAL) // This is to delay it every 0.1 seconds that is update
            }

         */
        viewModelScope.launch {
            try {
                while (true) {
                    val pos = playbackState.value?.currentPlaybackPosition

                    Log.d("SongViewModelCurPlayerPosition", curPlayerPosition.value.toString())
                    Log.d("SongViewModelPosition", pos.toString())


                    if (curPlayerPosition.value != pos) {
                        Log.d("SongViewModel", "Updating player position: $pos")
                        _curPlayerPosition.postValue(pos!!)
                        _curSongDuration.postValue(MusicService.curSongDuration)
                    }

                    delay(UPDATE_PLAYER_POSITION_INTERVAL)
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Coroutine exception: ${e.message}", e)
            }

        }
    }



}