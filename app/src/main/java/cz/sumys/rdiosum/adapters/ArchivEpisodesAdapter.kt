package cz.sumys.rdiosum.adapters
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.sumys.rdiosum.database.ArchivEpisode
import cz.sumys.rdiosum.databinding.ArchivItemBinding
import cz.sumys.rdiosum.fragments.ArchivFragment
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ArchivEpisodesAdapter (val clickListener: ListItemListener): RecyclerView.Adapter<ArchivEpisodesAdapter.ViewHolder>() {
    val log: Logger = LoggerFactory.getLogger(ArchivEpisodesAdapter::class.java)
    var data = listOf<ArchivEpisode>()
        set(value) {
            field = value //just pass the data
            notifyDataSetChanged() //notify recycler view that the data changed, reloads all data - bad
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position] // position in the list
        holder.bind(item, clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent) // inflate one item view
    }

    override fun getItemCount() = data.size // get messages count

    /**
     * ViewHolder contains logic for viewing messages
     * Pass the layout, via binding.root
     */
    class ViewHolder private constructor(val binding: ArchivItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val log: Logger = LoggerFactory.getLogger(ViewHolder::class.java)

        // assign views
        private val episodeNumber: TextView = binding.episodeNumber
        private val episodeName: TextView = binding.episodeName
        private val anchor: TextView = binding.anchor

        /**
         * Attach item data to binding
         */
        @SuppressLint("SetTextI18n")
        fun bind(item: ArchivEpisode, clickListener: ListItemListener) {
            anchor.text = item.episodeId.toString()
            episodeNumber.text = "${item.episodeNumber}. díl"
            episodeName.text = item.episodeName
            binding.clickListener = clickListener // actually bind the clicklistener to layout
        }

        /**
         * Inflates list element
         */
        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context) //here always pass parent when in recycler view adapter
                val binding = ArchivItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

/**
 * Class for retrieving the clicked episode`s Id
 */
class ListItemListener(val clickListener: (episodeId: Long) -> Unit) { //that means it takes function which takes Long and returns nothing
    fun onClick(episode: String) = clickListener(episode.toLong()) //assign the function onClick to clickListener lambda
}