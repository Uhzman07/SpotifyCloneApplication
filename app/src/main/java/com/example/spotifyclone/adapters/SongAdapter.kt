package com.example.spotifyclone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.R
import com.example.spotifyclone.data.entities.Song
import javax.inject.Inject

// This is the adapter that is to display our songs
/**
 * Diff Util is a tool that is updated anytime our list changes
 * It takes not of any item that may have changed
 * It helps to notify us of any possible change that may have occurred.
 */
class SongAdapter @Inject constructor(
    private val glide: RequestManager // Note that we are setting Glide as a request manager
) : BaseSongAdapter(R.layout.list_item){

    // Note that the variable "differ" is from the
    override val differ = AsyncListDiffer(this,diffCallback)


    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply {
            findViewById<TextView>(R.id.tvPrimary).text = song.title
            findViewById<TextView>(R.id.tvSecondary).text = song.subtitle
            glide.load(song.imageUrl).into(this.findViewById<ImageView>(R.id.ivItemImage))

            setOnClickListener {
                onItemClickListener?.let { click ->
                    click(song)
                }
            }

        }
    }

}