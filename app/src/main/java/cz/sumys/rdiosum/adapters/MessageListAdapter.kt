package cz.sumys.rdiosum.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.database.BaseMessage
import cz.sumys.rdiosum.databinding.ChatItemBinding
import cz.sumys.rdiosum.fragments.ChatFragment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*


class MessageListAdapter (): RecyclerView.Adapter<MessageListAdapter.ViewHolder>() {
    val log: Logger = LoggerFactory.getLogger(MessageListAdapter::class.java)
    var data = listOf<BaseMessage>()
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
    class ViewHolder private constructor(val binding: ChatItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val log: Logger = LoggerFactory.getLogger(ViewHolder::class.java)

        // assign views
        private val messageDate: TextView = binding.messageDate
        private val messageSender: TextView = binding.messageNickname
        private val messageIcon: ImageView = binding.messageImage
        private val messageText: TextView = binding.messageText
        private val messageTimestamp: TextView = binding.messageTimestamp

        // format the time instances
        private val dateFormatter = SimpleDateFormat("dd. MM.", Locale.GERMANY)
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.GERMANY)

        /**
         * Attach item data to binding
         */
        fun bind(item: BaseMessage) {
            messageDate.text = dateFormatter.format(item.messageTimestamp*1000)
            messageSender.text = item.messageSender
            messageText.text = item.messageText
            messageTimestamp.text = timeFormatter.format(item.messageTimestamp*1000)
            if (item.messageSender in SUMYS_HEADQUARTERS) {
                messageIcon.setImageResource(R.drawable.punk)
            } else {
                messageIcon.setImageResource(R.drawable.ic_sumys_icon)
            }
        }

        /**
         * Inflates list element
         */
        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context) //here always pass parent when in recycler view adapter
                val binding = ChatItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
            private val SUMYS_HEADQUARTERS = arrayOf("Vojta", "Martus", "Mára", "Miky", "Dan", "Mýtus")
        }
    }



}