package com.example.spotifyclone.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifyclone.R
import com.example.spotifyclone.adapters.SongAdapter
import com.example.spotifyclone.data.remote.MusicDatabase
import com.example.spotifyclone.exoplayer.FirebaseMusicSource
import com.example.spotifyclone.exoplayer.MusicNotificationManager
import com.example.spotifyclone.exoplayer.MusicService
import com.example.spotifyclone.other.Status
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Note that @AndroidEntryPoint is from Dagger Hilt
 * It is an annotation used when we want to inject into a component such as Fragments, Activity, Service
 */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    // Note that here, we do not want to bind the view Model to our lifecycle activity
    lateinit var mainViewModel: MainViewModel

    private lateinit var musicNotificationManager: MusicNotificationManager











    /// Note that we have to inject the SongAdapter since it also contains some injections in it and
    // then the scope of an Adapter is not as wide as that of a ViewModel
    @Inject
    lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        // Then to bind our viewModel to the lifecycle
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        setUpRecyclerView()
        subscribeToObservers()
        songAdapter.setItemClickListener {
            mainViewModel.playOrToggleSong(it)
        }


    }

    private fun setUpRecyclerView() = view?.findViewById<RecyclerView>(R.id.rvAllSongs)?.apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result ->
            // To perform a switch condition on the status of the result derived
            when(result.status){

                Status.SUCCESS -> {
                    //Toast.makeText(requireContext(),"Success",Toast.LENGTH_LONG).show()
                    view?.findViewById<ProgressBar>(R.id.allSongsProgressBar)?.isVisible = false
                    result.data?.let { songs ->
                        Log.d("HomeFragment", "Number of songs: ${songs.size}")
                        songAdapter.songs = songs // This will submit the list to the listDiffer
                    }


                }
                Status.ERROR -> Unit

                Status.LOADING ->{
                    Log.d("Error", result.data.toString())

                    view?.findViewById<ProgressBar>(R.id.allSongsProgressBar)?.isVisible = true
                    //Toast.makeText(requireContext(),"Loading",Toast.LENGTH_LONG).show()

                }

            }

        }
    }


}