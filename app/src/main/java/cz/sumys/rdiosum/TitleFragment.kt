package cz.sumys.rdiosum

import android.annotation.SuppressLint
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
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import cz.sumys.rdiosum.SumysApplication.Companion.CHANNEL_1_ID
import cz.sumys.rdiosum.databinding.FragmentTitleBinding


class TitleFragment : Fragment() {

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
        viewModel = ViewModelProvider(this).get(TitleViewModel::class.java)

        // -----------------------------------------------------------------------------------------
        // Handler setup for periodic tasks
        handler = Handler(Looper.getMainLooper())
        val runnableCode = object: Runnable { // periodic checking of radio status
            override fun run() {
                Log.i("TitleFragment", "Handler run")
                internetCheck()
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

        // miscellaneous
        binding.sumysBanner.setOnClickListener {
            Toast.makeText(context, "Blbě čumíš", Toast.LENGTH_SHORT).show()
        }

        // -----------------------------------------------------------------------------------------
        // OBSERVERS ATTACHMENT

        // play button state
        viewModel.playButtonState.observe(viewLifecycleOwner, { playButtonState ->
            Log.i("TitleFragment", "Play button pressed, value = $playButtonState")
            if (playButtonState == "stop") {
                if (!viewModel.playing) {
                    initializePlaying(runnableCode)
                } else binding.playButton.setImageResource(R.drawable.stop_button_white)
            } else {
                handler.removeCallbacksAndMessages(null) // disable periodic checking
                stopPlaying()
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

        // setup Media Player
        viewModel.setMediaPlayer()

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
            false
        } else {
            context?.let { viewModel.downloadXSPF(it) }
            true
        }
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Play button was pressed and radio should start playing
     */
    private fun initializePlaying(runnableCode: Runnable) {
        viewModel.playing = true
        val isInternet = internetCheck()
        handler.postDelayed(runnableCode, 1000) // start periodic checking of radio status
        if (isInternet) {
            viewModel.initializeStream() // start streaming
            // change main button icon
            binding.playButton.setImageResource(R.drawable.stop_button_white)
        }
    }

    /**
     * Stop button was pressed and radio should stop playing
     */
    fun stopPlaying() {
        Log.i("TitleFragment", "stopPlaying() called")
        viewModel.playing = false
        viewModel.stopStreaming()
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
        val info = context?.let { viewModel.parseXSPF(it) }
        binding.song.text = "${info?.get(0)} - ${info?.get(1)}"
        binding.currentListeners.text = "Rádio právě poslouchá ${info?.get(2)} lidí"
        info?.get(0)?.let { sendOnSongChannel(it, info[1]) } // send info to notification
    }


    /**
     * Audio should be playing now so its time to display song info
     */
    private fun showInfo() {
        binding.song.visibility = View.VISIBLE
        binding.bandzoneBanner.visibility = View.VISIBLE
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
     * Send notification
     */
    private fun sendOnSongChannel(author: String, song: String) {
        val title = "Rádio Sumýš"
        val message = "$author - $song"

        // content intent when user presses the notification body
        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.action = Intent.ACTION_MAIN
        activityIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(context, 0, activityIntent, 0)

        // action intent for stop button
        val actionStopIntent = Intent(context, ActionReceiver::class.java)
        actionStopIntent.putExtra("action", "stop")
        val pActionStopIntent = PendingIntent.getBroadcast(context,
                1, actionStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // action intent for notification dismiss
        val actionDeleteIntent = Intent(context, ActionReceiver::class.java)
        actionDeleteIntent.putExtra("action", "delete")
        val pActionDeleteIntent = PendingIntent.getBroadcast(context,
                1, actionDeleteIntent, 0)

        // picture and its bitmap
        val picture = BitmapFactory.decodeResource(resources, R.drawable.sumys_notification)

        // define the notification looks
        val notification = context?.let { NotificationCompat.Builder(it, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_sumys_icon)
                .setLargeIcon(picture)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                )
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_notif_stop, "stop", pActionStopIntent)
                .setDeleteIntent(pActionDeleteIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
        }

        if (notification != null) {
            notificationManager.notify(1, notification)
        }
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
            Log.e("TitleFragment", "Failed to launch intent to bandzone web page")
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Broadcast receiver when the notification is dismissed
     */
    private val broadcastStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.playButtonPressed(true)
        }
    }

    override fun onAttach(context: Context) {
        // register broadcast receivers
        context.registerReceiver(broadcastStopReceiver, IntentFilter("NOTIFICATION_DISMISSED"))
        super.onAttach(context)
    }

    // ---------------------------------------------------------------------------------------------
    companion object {
        private const val INFO_PERIOD: Long = 8000
    }
}

