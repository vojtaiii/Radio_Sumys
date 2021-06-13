package cz.sumys.rdiosum.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.database.BaseMessage
import cz.sumys.rdiosum.database.NewsMessage
import cz.sumys.rdiosum.databinding.ChatItemBinding
import cz.sumys.rdiosum.databinding.NewsItemBinding
import cz.sumys.rdiosum.fragments.ChatFragment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*


class NewsListAdapter (): RecyclerView.Adapter<NewsListAdapter.ViewHolder>() {
    val log: Logger = LoggerFactory.getLogger(NewsListAdapter::class.java)
    var data = listOf<NewsMessage>()
        set(value) {
            field = value //just pass the data
            notifyDataSetChanged() //notify recycler view that the data changed, reloads all data - bad
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position] // position in the list
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent) // inflate one item view
    }

    override fun getItemCount() = data.size // get messages count

    /**
     * ViewHolder contains logic for viewing messages
     * Pass the layout, via binding.root
     */
    class ViewHolder private constructor(val binding: NewsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val log: Logger = LoggerFactory.getLogger(ViewHolder::class.java)

        // assign views
        private val newsDate: TextView = binding.newsDate
        private val newsText: TextView = binding.newsMessage
        private val newsImage: ImageView = binding.newsImage

        // format the time instances
        private val dateFormatter = SimpleDateFormat("dd. MM. HH:mm", Locale.GERMANY)

        /**
         * Attach item data to binding
         */
        fun bind(item: NewsMessage) {
            newsDate.text = dateFormatter.format(item.newsTimestamp)
            newsText.text = item.newsText
            if (item.newsImage != "") {
                // handle the image source with the Picasso library
                    Picasso.get()
                        .load(item.newsImage)
                        .resize(800, 520)
                        .centerCrop()
                        .into(newsImage)
            } else {
                newsImage.visibility = View.GONE
            }
        }

        /**
         * Inflates list element
         */
        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context) //here always pass parent when in recycler view adapter
                val binding = NewsItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }



}