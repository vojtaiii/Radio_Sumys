package cz.sumys.rdiosum.fragments

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import cz.sumys.rdiosum.utilities.ActionReceiver
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.applications.SumysApplication.Companion.CHANNEL_1_ID
import cz.sumys.rdiosum.viewmodels.TitleViewModel
import cz.sumys.rdiosum.utilities.Utils
import cz.sumys.rdiosum.activities.MainActivity
import cz.sumys.rdiosum.databinding.FragmentTitleBinding
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TitleFragment : Fragment() {
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var viewModel: TitleViewModel
    private lateinit var binding: FragmentTitleBinding
    private lateinit var handler: Handler
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate<FragmentTitleBinding>(
            inflater, R.layout.fragment_title,
            container, false
        )

        // notify the fragment the options are here
        setHasOptionsMenu(true)

        // initialize the view model
        viewModel = activity?.let { ViewModelProvider(it).get(TitleViewModel::class.java) }!!

        // -----------------------------------------------------------------------------------------
        // Handler setup for periodic tasks
        handler = Handler(Looper.getMainLooper())
        val runnableCode = object: Runnable { // periodic checking of radio status
            override fun run() {
                log.debug("Handler run")
                internetCheck()
                context?.let { viewModel.sendDataToSumysService(it) }
                handler.postDelayed(this, INFO_PERIOD)
            }
        }

        // -----------------------------------------------------------------------------------------
        // LISTENERS ATTACHMENT

        // play button state
        binding.playButton.setOnClickListener {
            val isInternet = context?.let { Utils.isOnline(it) } == true
            viewModel.playButtonPressed(isInternet) }

        // creates an intent to bandzone search page
        binding.bandzoneBanner.setOnClickListener {
            bandzoneIntent()
        }

        // pressing the spinner should stop the process
        binding.progressBar.setOnClickListener {
            context?.let { it1 -> stopProcess(it1) }
        }

        // miscellaneous
        binding.sumysBanner.setOnClickListener {
            Toast.makeText(context, "Blbě čumíš", Toast.LENGTH_SHORT).show()
        }

        // -----------------------------------------------------------------------------------------
        // OBSERVERS ATTACHMENT

        // play button state
        viewModel.playButtonState.observe(viewLifecycleOwner, { playButtonState ->
           log.debug("Play button pressed, value = $playButtonState")
            if (playButtonState == "stop") {
                if (!viewModel.playing) {
                    context?.let { initializePlaying(runnableCode, it) }
                } else binding.playButton.setImageResource(R.drawable.pst_button)
            } else {
                handler.removeCallbacksAndMessages(null) // disable periodic checking
                context?.let { stopPlaying(it) }
            }
        })

        // spinning active
        viewModel.spinning.observe(viewLifecycleOwner, { spinning ->
            if (spinning) {
                startSpinning()
            } else {
                stopSpinning()
            }
        })

        // info content downloaded
        viewModel.downloaded.observe(viewLifecycleOwner, { downloaded ->
            if (downloaded) parseInfo()
        })
        // -----------------------------------------------------------------------------------------

        // setup notifications manager
        notificationManager = context?.let { NotificationManagerCompat.from(it) }!!
        mediaSession = MediaSessionCompat(requireContext(), "sumys_tag")

        // register receivers
        registerReceivers(requireContext())

        return binding.root
    }

    // ---------------------------------------------------------------------------------------------
    // inflate the menu resource file
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.options_menu, menu)
    }

    // Options menu item pressed
    // navigate to the fragment that has the same id as the selected menu item
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.
                onNavDestinationSelected(item, requireView().findNavController())
                || super.onOptionsItemSelected(item)
    }
    // ---------------------------------------------------------------------------------------------

    /**
     * Performs check whether a internet connection is available when the fragment is
     * created. If true, collects data about current radio state from server.
     */
    private fun internetCheck(): Boolean {
        // check internet
        val isInternet = context?.let { Utils.isOnline(it) } == true
        return if (!isInternet) {
            context?.let { Toast.makeText(it, "Musí ti fungovat internet", Toast.LENGTH_SHORT).show() }
            log.debug("Internet check: false")
            false
        } else {
            context?.let { viewModel.downloadXSPF(it) }
            log.debug("Internet check: true")
            true
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Check if the background streaming service has not been killed in the meantime
     */
    override fun onResume() {
        super.onResume()

        // check if background service has not been killed
        if (!viewModel.sumysServiceRunning()) viewModel.setToPlay()
    }

    /**
     * Play button was pressed and radio should start playing
     */
    private fun initializePlaying(runnableCode: Runnable, context: Context) {
        viewModel.playing = true
        val isInternet = internetCheck()
        handler.postDelayed(runnableCode, 1000) // start periodic checking of radio status
        if (isInternet) {
            viewModel.initializeStream(context) // start streaming
            // change main button icon
            binding.playButton.setImageResource(R.drawable.pst_button)
        }
    }

    /**
     * Stop button was pressed and radio should stop playing
     */
    private fun stopPlaying(context: Context) {
        log.debug("stopPlaying() called")
        viewModel.playing = false
        viewModel.stopStreaming(context)

        notificationManager.cancel(1) // hide the notification
        // change main button icon
        binding.playButton.setImageResource(R.drawable.play_button)
        // make text views related to streamed content invisible
        binding.song.visibility = View.INVISIBLE
        binding.bandzoneBanner.visibility = View.INVISIBLE
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Make the parsed .xspf data appear in the UI
     */
    @SuppressLint("SetTextI18n")
    private fun parseInfo() {
        binding.song.text = "${viewModel.band} - ${viewModel.song}"
        binding.currentListeners.text = "Rádio právě poslouchá ${viewModel.currentListeners} lidí"
    }


    /**
     * Audio should be playing now so its time to display song info
     */
    private fun showInfo() {
        binding.song.visibility = View.VISIBLE
        binding.bandzoneBanner.visibility = View.VISIBLE
    }

    /**
     * The spinner was clicked and the process should be stopped
     */
    private fun stopProcess(context: Context) {
        log.debug("Spinner clicked, cancelling process")
        handler.removeCallbacksAndMessages(null) // disable periodic checking
        stopPlaying(context)
        stopSpinning()
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Control circular progress bar behaviour
     */
    private fun startSpinning() {
        binding.playButton.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
    }
    private fun stopSpinning() {
        if (viewModel.playing) showInfo()
        binding.progressBar.visibility = View.INVISIBLE
        binding.playButton.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Creates an intent with bandzone web page
     */
    private fun bandzoneIntent() {
        val author = viewModel.bandzoneAuthor
        val url = "https://bandzone.cz/hledani.html?q=$author"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        try {
            startActivity(i)
        } catch (e: Exception) {
            log.error("Failed to launch intent to bandzone web page")
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Broadcast receiver for killing the stream
     */
    private val streamKilledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            log.debug("streamKilledReceiver reached")
            handler.removeCallbacksAndMessages(null)
            viewModel.setToPlay()
        }
    }

    /**
     * Broadcast receiver for spinning info
     */
    private val broadcastSpinningReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.spinningDone()
        }
    }

    private fun registerReceivers(context: Context) {
        log.debug("Receivers registered")
        // register broadcast receivers
        LocalBroadcastManager.getInstance(context).registerReceiver(
                streamKilledReceiver, IntentFilter("NOTIFICATION_DISMISSED"))
        LocalBroadcastManager.getInstance(context).registerReceiver(
                broadcastSpinningReceiver, IntentFilter("spinning"))
    }

    // ---------------------------------------------------------------------------------------------
    companion object {
        private const val INFO_PERIOD: Long = 8000
    }
}

