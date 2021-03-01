package com.example.rdiosum

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.rdiosum.databinding.FragmentTitleBinding

class TitleFragment : Fragment() {

    private lateinit var viewModel: TitleViewModel
    private lateinit var binding: FragmentTitleBinding

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

        // -----------------------------------------------------------------------------------------
        // LISTENERS ATTACHMENT

        // play button state
        binding.playButton.setOnClickListener {
            val isInternet = context?.let { Utils.isOnline(it) } == true
            viewModel.playButtonPressed(isInternet) }

        // -----------------------------------------------------------------------------------------
        // OBSERVERS ATTACHMENT

        // play button state
        viewModel.playButtonState.observe(viewLifecycleOwner, { playButtonState ->
            if (playButtonState == "stop") {
                initializePlaying()
            } else stopPlaying()
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

        // setup Media Player
        viewModel.setMediaPlayer()

        // perform initial internet check
        initialInternetCheck()

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
    // -----------------------------------------------------------------------------------------

    /**
     * Performs the initial check whether a internet connection is available when the fragment is
     * created. If true, collects data about current radio state from server and displays
     * number of listeners.
     */
    @SuppressLint("SetTextI18n")
    private fun initialInternetCheck() {
        // check internet
        val isInternet = context?.let { Utils.isOnline(it) } == true
        // download xspf file
        if (isInternet) {
            context?.let { viewModel.downloadXSPF(it) }
        }
    }

    private fun internetCheck(): Boolean {
        // check internet
        val isInternet = context?.let { Utils.isOnline(it) } == true
        return if (!isInternet) {
            Toast.makeText(context, "Musí ti fungovat internet", Toast.LENGTH_SHORT).show()
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
    private fun initializePlaying() {
        val isInternet = internetCheck()
        if (isInternet) {
            viewModel.initializeStream() // start streaming
            // change main button icon
            binding.playButton.setImageResource(R.drawable.stop_button_white)
        }
    }

    /**
     * Stop button was pressed and radio should stop playing
     */
    private fun stopPlaying() {
        viewModel.stopStreaming()
        // change main button icon
        binding.playButton.setImageResource(R.drawable.play_button)
        // make text views related to streamed content visible
        binding.nowPlaying.visibility = View.INVISIBLE
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
    }


    /**
     * Audio should be playing now so its time to display song info
     */
    private fun showInfo() {
        binding.nowPlaying.visibility = View.VISIBLE
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
        showInfo()
        binding.progressBar.visibility = View.INVISIBLE
        binding.playButton.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------------------------------------------

    companion object {
        private const val INFO_PERIOD: Long = 5000
    }
}

