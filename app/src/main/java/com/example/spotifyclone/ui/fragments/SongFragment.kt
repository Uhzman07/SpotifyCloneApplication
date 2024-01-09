package com.example.spotifyclone.ui.fragments

import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.R
import com.example.spotifyclone.adapters.BaseSongAdapter
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.toSong
import com.example.spotifyclone.other.Status
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import com.example.spotifyclone.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint // This is because we are going to inject something like the Glide instance
class SongFragment : Fragment(R.layout.fragment_song) {

    @Inject
    lateinit var glide : RequestManager // That is to inject Glide

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel : SongViewModel by viewModels()

    private var playbackState:PlaybackStateCompat? =null


    private var curPlayingSong: Song? = null

    var privView : View? = null

    private var shouldUpdateSeekbar = true

    private val dateFormatter = SimpleDateFormat("mm:ss", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Note that the View Model is bounded to the activity and not the Fragment so we have to use the requireActivity() for it
        // Then we make use of the get method to get the actual ViewModel that we want to instantiate
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()

        privView = view

        /*


        privView?.findViewById<SeekBar>(R.id.seekBar)?.min = 0
        privView?.findViewById<SeekBar>(R.id.seekBar)?.progress = 0

        privView?.findViewById<TextView>(R.id.tvCurTime)?.text = "00:05"

         */

        privView?.findViewById<ImageView>(R.id.ivPlayPauseDetail)?.setOnClickListener {
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it,true)
            }
        }

        privView?.findViewById<SeekBar>(R.id.seekBar)?.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // We need to check if the progress change was initiated by the user him self
                // Note that the new update has "seekBar", "progress" and "fromUser" as "p0", "p1" and "p2" respectively in the argument(I just changed for comprehension)
                if(fromUser){
                    //val time = dateFormatter.format(progress)
                   // Log.d("Duration2", time)
                    setCurrentTimeToTextView(progress.toLong())
                    //view?.findViewById<TextView>(R.id.tvCurTime)?.text = time




                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                // This is when we do not want our seek bar to be updated when it is touched
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // Then to update the seek bar when the seek bar movement is stopped or if it is actually released
                p0?.let{
                    mainViewModel.seekTo(it.progress.toLong())
                    //Log.d("Duration tag", it?.progress!!.toLong().toString())


                    //val time = dateFormatter.format(it.progress)
                    //Log.d("Duration2", time)
                    //setCurrentTimeToTextView(progress.toLong())
                    shouldUpdateSeekbar = true
                    //view?.findViewById<TextView>(R.id.tvCurTime)?.text = time
                }



            }

        })

        // To Skip to Previous Song
        privView?.findViewById<ImageView>(R.id.ivSkipPrevious)?.setOnClickListener{
            mainViewModel.skipToPreviousSong()
        }

        // To skip to the Next Song
        privView?.findViewById<ImageView>(R.id.ivSkip)?.setOnClickListener {
            mainViewModel.skipToNextSong()

        }





    }
    private fun updateTitleAndSongImage(song:Song){
        val title = "${song.title} - ${song.subtitle}"
        privView?.findViewById<TextView>(R.id.tvSongName)?.text = title
        glide.load(song.imageUrl).into(privView?.findViewById<ImageView>(R.id.ivSongImage)!!) // Be careful about the View type please
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){
            it?.let{ result ->
                when(result.status){
                    Status.SUCCESS -> {
                        result?.data?.let{ songs ->
                            if(curPlayingSong == null && songs.isNotEmpty()){
                                curPlayingSong = songs[0]
                                updateTitleAndSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }

            }
        }
        mainViewModel.curPlayingSong.observe(viewLifecycleOwner){
            if(it == null) return@observe
            curPlayingSong = it.toSong() // Remember to check this method out please
            updateTitleAndSongImage(curPlayingSong!!)

        }

        /**
         * Then to change the image and then update the seekbar
         */
        mainViewModel.playbackState.observe(viewLifecycleOwner){
            playbackState = it
            view?.findViewById<ImageView>(R.id.ivPlayPauseDetail)?.setImageResource(
                if(playbackState?.isPlaying == true){
                    R.drawable.ic_pause
                }
            else{
                R.drawable.ic_play
                }
            )
            // Check this please
            view?.findViewById<SeekBar>(R.id.seekBar)?.progress = it?.position?.toInt() ?: 0




        }


        songViewModel.curPlayerPosition.observe(viewLifecycleOwner){
            if(shouldUpdateSeekbar){ // Check this "shouldUpdateSeekbar"

                view?.findViewById<SeekBar>(R.id.seekBar)?.progress = it.toInt()
                setCurrentTimeToTextView(it)
            }
        }

        songViewModel.curSongDuration.observe(viewLifecycleOwner){

            view?.findViewById<SeekBar>(R.id.seekBar)?.max = it.toInt()
            Log.d("Main Boolean", shouldUpdateSeekbar.toString())
            Log.d("Main Duration", it.toString())

            // This is used to test the code

            //var finalTime = dateFormatter.format(it)
            //Log.d("Duration of song", finalTime)

            //val maxDuration = it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
           // view?.findViewById<SeekBar>(R.id.seekBar)?.max = maxDuration

            val dateFormat = SimpleDateFormat("mm:ss",  Locale.getDefault())
            view?.findViewById<TextView>(R.id.tvSongDuration)?.text = dateFormat.format(it)

        }



    }

    private fun setCurrentTimeToTextView(ms: Long?) {
        // Then to create a format for this in terms of minutes and seconds
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        view?.findViewById<TextView>(R.id.tvCurTime)?.text = dateFormat.format(ms)


    }


}