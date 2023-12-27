package com.example.spotifyclone

import android.app.NotificationManager
import android.content.Context
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.adapters.SwipeSongAdapter
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.databinding.ActivityMainBinding
import com.example.spotifyclone.exoplayer.MusicService
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.toSong
import com.example.spotifyclone.other.Status
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
/*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
 */
//import com.example.spotifyclone.ui.theme.SpotifyCloneTheme
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity  : AppCompatActivity(), EasyPermissions.PermissionCallbacks{

    // Note that we are instantiating the MainViewModel like this because we want to bind it to our main lifecycle activity
    private val mainViewModel : MainViewModel by viewModels()



    /**
     * To inject the swipeSongAdapter
     */
    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide : RequestManager

    private var curPlayingSong: Song? = null

    var getIT1 = false

    private var playbackState : PlaybackStateCompat? = null

    // For the binding
    private  lateinit var binding : ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        subscribeToObservers()

        /*

        // musicNotificationManager.
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(applicationContext, "channel_id")
            .setContentText("This is some content text")
            .setContentTitle("Hello World!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(1, notif)

         */

        getIT1 = hasPermissions()

        // Use getIT1 as needed

        Companion.getIT2 = getIT1

        requestNotificationPermissions()



        /**
         * Note
         * A ViewPager is a UI component that allows users to flip left and right through a set of pages.
         * It is commonly used for implementing swipeable tabs or for creating image galleries where users can swipe between images.
         * Each page in a ViewPager is typically represented by a Fragment or a View
         */
        // The to set the adapter of the view Pager
        binding.vpSong.adapter = swipeSongAdapter

        // To then be able to change the song when we swipe the view pager
        binding.vpSong.registerOnPageChangeCallback(object: OnPageChangeCallback(){
            // This is an implemented method
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if(playbackState?.isPlaying == true){
                    // Note that the "position" representation here is the part where we swiped our song to
                    // Then we are going to get the song using the swipe song adapter which is the adapter that is for the viewPager to get the song at the position swipped to
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                }
                else{ // That is if it is not playing then we just want to set the curPlaying song to it
                    curPlayingSong = swipeSongAdapter.songs[position]

                }
            }
        })

        binding.ivPlayPause.setOnClickListener {
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it,true)
            }
        }

        /**
         * To get to know the layout that is occupying the navHostFragment
         */
        findNavController(R.id.navHostFragment).addOnDestinationChangedListener{_, destination ,_ ->
            // To check the fragment which we are navigated to based on the ID
            when(destination.id){
                R.id.songFragment -> hideBottomBar()
                R.id.homeFragment -> showBottomBar()
                else-> showBottomBar() // That is to show the bottom bar by default
            }


        }

        /**
         * Then to be able to navigate
         */
        swipeSongAdapter.setItemClickListener {
            findNavController(R.id.navHostFragment).navigate(
                R.id.songFragment
            )
        }



    }

    private fun hideBottomBar(){
        binding.ivCurSongImage.isVisible = false
        binding.vpSong.isVisible = false
        binding.ivPlayPause.isVisible = false

    }

    private fun showBottomBar(){
        binding.ivCurSongImage.isVisible = true
        binding.vpSong.isVisible = true
        binding.ivPlayPause.isVisible = true

    }
    private fun switchViewPagerToCurrentSong(song:Song){
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if(newItemIndex!=-1){ // Since it is expected to return -1 if the song does not exist in the list
            binding.vpSong.currentItem = newItemIndex // This is to set the Viewpager to the current song in the list, this is done by setting the index
            curPlayingSong = song

        }
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(this){
            it?.let { result ->
                when(result.status){
                    Status.SUCCESS -> {
                        result.data?.let{ songs ->
                            swipeSongAdapter.songs = songs
                            if(songs.isNotEmpty()){
                                glide.load((curPlayingSong ?:songs[0]).imageUrl).into(binding.ivCurSongImage)
                            }
                            switchViewPagerToCurrentSong(curPlayingSong?: return@observe) // We want to set the current playing song and if it is null, we want to return out of the observe block
                        }

                    }
                    Status.ERROR -> Unit
                    Status.LOADING ->Unit
                }

            }


        }
        mainViewModel.curPlayingSong.observe(this){
            if(it==null) return@observe

            curPlayingSong = it.toSong() // This will convert the MediaMetadataCompat into a Song object
            glide.load(curPlayingSong?.imageUrl).into(binding.ivCurSongImage)
            switchViewPagerToCurrentSong(curPlayingSong?:return@observe)


        }
        /**
         * To chnage the icon based on the Playback State
         */
        mainViewModel.playbackState.observe(this){
            playbackState = it
            binding.ivPlayPause.setImageResource(
                if(playbackState?.isPlaying == true){
                    R.drawable.ic_pause
                }else{
                    R.drawable.ic_play
                }
            )
        }

        mainViewModel.isConnected.observe(this){
            // Assuming you have a reference to a View (e.g., a layout or a specific view in your activity)
            // This here is representing the constraint layout that we have
            val rootView: View = findViewById(R.id.rootLayout)

            it?.getContentIfNotHandled()?.let { result ->
                when(result.status){
                    Status.ERROR -> Snackbar.make(
                        rootView,
                        result.message?:"An unknown error occured",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this){
            // Assuming you have a reference to a View (e.g., a layout or a specific view in your activity)
            // This here is representing the constraint layout that we have
            val rootView: View = findViewById(R.id.rootLayout)

            it?.getContentIfNotHandled()?.let { result ->
                when(result.status){
                    Status.ERROR -> Snackbar.make(
                        rootView,
                        result.message?:"An unknown error occured",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }
    companion object{
        var getIT2: Boolean = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d("Notification", "Granted Already")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionDenied(this, android.Manifest.permission.POST_NOTIFICATIONS)){
            Log.d("Notification", "Denied")

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissions(){
        EasyPermissions.requestPermissions(this,
        "Notification Permissions  - This permissions are used for the notification",
        1,
        android.Manifest.permission.POST_NOTIFICATIONS)
        //android.Manifest.permission.ACCESS_NOTIFICATION_POLICY)
    }

    fun hasPermissions(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
            //android.Manifest.permission.ACCESS_NOTIFICATION_POLICY
        )
    }



}

/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpotifyCloneTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SpotifyCloneTheme {
        Greeting("Android")
    }
}

 */