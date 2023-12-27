package com.example.spotifyclone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifyclone.R
import com.example.spotifyclone.data.entities.Song

/**
 * The Song Adapter and the BaseSongAdapter are different in such a way that they make use of different layout
 * This is a kind of abstract class that is supposed to be the adapter for any xml layout
 * All we just need to do is to insert the layout ID in the constructor, this layoutId is an Integer
 */
abstract class BaseSongAdapter(
    private val layoutId : Int
):RecyclerView.Adapter<BaseSongAdapter.SongViewHolder>(){

    class SongViewHolder(itemView : View): RecyclerView.ViewHolder(itemView)

    // For the Diff Util, we can specify it by treating is as an Object with a given type of what it is to hold i.e Song in this instance
    // This is the CallBack for the AsyncListDiffer
    protected val diffCallback = object : DiffUtil.ItemCallback<Song>(){
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            // Note that to check the content of the items, we can make use of their unique "HashCode" that is hash value
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    /**
     * To create the list differ
     * Note that "this" here refers to the recycler view adapter i.e this class
     * The other argument "diffCallback" refers to the diffCallback that had just been created
     */
    /**
     * Also note that referencing "this" could also lead to a problem because the Song Adapter might not be create before the ListDiffer
     * so we can make it protected and abstract so as to avoid a multi threading issue
     */
    protected abstract val differ: AsyncListDiffer<Song>

    //private val differ = AsyncListDiffer(this, diffCallback)

    var songs: List<Song>
        get() = differ.currentList // This means the list in the differ will be returned when we try to access the song list
        set(value) = differ.submitList(value) // This is used to set the list of songs in Differ if the list of songs is changed

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            /**
             * The LayoutInflater is responsible for inflating (creating) views from XML layout resource files
             */
            // So instead of using the layout directly, we can make use of the layout that was passed in
            LayoutInflater.from(parent.context).inflate(
                layoutId,
                parent, //  The ViewGroup to which the inflated view will be attached. In this case, it's the RecyclerView's parent view.
                false
            )
        )
    }
    // Note that we do not need the onBindViewHolder here

    // This is the Song listener
    protected var onItemClickListener: ((Song) -> Unit)? =null

    /**
     * @param listener this takes the listener created above in form of a lambda function as a parameter
     */
    fun setItemClickListener(listener :(Song) -> Unit){
        onItemClickListener = listener
    }

    override fun getItemCount(): Int {
        return songs.size
    }

}