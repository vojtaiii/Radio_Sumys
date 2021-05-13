package cz.sumys.rdiosum.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.adapters.MessageListAdapter
import cz.sumys.rdiosum.database.SumysDatabase
import cz.sumys.rdiosum.databinding.FragmentChatBinding
import cz.sumys.rdiosum.factories.ChatViewModelFactory
import cz.sumys.rdiosum.utilities.Utils
import cz.sumys.rdiosum.viewmodels.ChatViewModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ChatFragment : Fragment() {
    val log: Logger = LoggerFactory.getLogger(ChatFragment::class.java)
    lateinit var chatViewModel: ChatViewModel
    private lateinit var binding: FragmentChatBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // binding
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_chat, container, false
        )

        // create viewModel, pass it application and databaseDAO reference
        val napplication = requireNotNull(this.activity).application
        val dataSource = SumysDatabase.getInstance(napplication).sumysDatabaseDao
        val viewModelFactory = ChatViewModelFactory(dataSource, napplication)
        chatViewModel = ViewModelProvider(this, viewModelFactory).get(ChatViewModel::class.java)

        // get reference to adapter
        val adapter = MessageListAdapter()
        // set layout manager
        val manager = LinearLayoutManager(context)
        // assign the adapter to recycler view
        binding.chatRecycler.adapter = adapter
        binding.chatRecycler.layoutManager = manager
        binding.lifecycleOwner = this

        //------------------------------------------------------------------------------------------
        // observe when recycler cant scroll anymore in up direction (-1) and post new request
        binding.chatRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(-1) && newState != RecyclerView.SCROLL_STATE_IDLE) {
                    if (System.currentTimeMillis() - chatViewModel.timeOfLastRefresh < PREVENT_PERIOD) return
                    log.debug("Recycler reached top")
                    chatViewModel.hearChat(true)
                    chatViewModel.timeOfLastRefresh = System.currentTimeMillis()
                }
            }
        })
        //------------------------------------------------------------------------------------------

        // erase the database when fragment is opened
        chatViewModel.clearDatabase()

        //------------------------------------------------------------------------------------------
        // observe the database, when something is changed, update the recycler view
        chatViewModel.messages.observe(viewLifecycleOwner, {
            it?.let {
                adapter.data = it
            }
        })

        // observe when new messages are stored and scroll to the last one
        // the operation has to be delayed as adapter takes some time to retrieve messages from
        // the database
        chatViewModel.finishedResponse.observe(viewLifecycleOwner, { finishedResponse ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (finishedResponse) binding.chatRecycler.scrollToPosition(adapter.itemCount - 1)
            },
                    300)
        })

        // observe the spinning live data and show spinner
        chatViewModel.spinning.observe(viewLifecycleOwner, { spinning ->
            if (spinning) {
                binding.spinner.visibility = View.VISIBLE
            } else {
                binding.spinner.visibility = View.INVISIBLE
            }
        })
        //------------------------------------------------------------------------------------------
        // send message
        binding.sendButton.setOnClickListener {
            if (!internetCheck()) return@setOnClickListener
            val nick = binding.sendNickname.text.toString()
            val msg = binding.sendText.text.toString()
            if (!chatViewModel.checkNickname(nick)) { // nick check
                Toast.makeText(context, R.string.check_nickname, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!chatViewModel.checkMessage(msg)) { // message check
                Toast.makeText(context, R.string.check_message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            chatViewModel.mluvChat(nick, msg, System.currentTimeMillis().toString())
            context?.let { it1 -> clearUponSend(it1) }
        }

        //------------------------------------------------------------------------------------------
        // initial database request
        if (internetCheck()) chatViewModel.hearChat(false)
        //------------------------------------------------------------------------------------------

        return binding.root
    }

    private fun clearUponSend(context: Context) {
        // hide keyboard
        // Only runs if there is a view that is currently focused
        activity?.currentFocus?.let { view ->
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        binding.sendText.setText("")
    }

    /**
     * Performs check whether a internet connection is available.
     */
    private fun internetCheck(): Boolean {
        // check internet
        val isInternet = context?.let { Utils.isOnline(it) } == true
        return if (!isInternet) {
            context?.let { Toast.makeText(it, "Musí ti fungovat internet", Toast.LENGTH_SHORT).show() }
            log.debug("Internet check: false")
            false
        } else {
            log.debug("Internet check: true")
            true
        }
    }

    companion object {
        private const val PREVENT_PERIOD = 2000L
    }
}