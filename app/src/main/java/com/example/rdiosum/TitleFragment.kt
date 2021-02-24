package com.example.rdiosum

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.rdiosum.databinding.FragmentTitleBinding

class TitleFragment : Fragment() {

    private lateinit var viewModel: TitleViewModel
    private lateinit var binding: FragmentTitleBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate<FragmentTitleBinding>(inflater, R.layout.fragment_title,
            container, false)

        // notify the fragment the options are here
        setHasOptionsMenu(true)

        // initialize the view model
        viewModel = ViewModelProvider(this).get(TitleViewModel::class.java)

        // -----------------------------------------------------------------------------------------
        // LISTENERS ATTACHMENT

        // play button state
        binding.playButton.setOnClickListener { viewModel.playButtonPressed() }

        // -----------------------------------------------------------------------------------------
        // OBSERVERS ATTACHMENT

        // play button state
        viewModel.playButtonState.observe(viewLifecycleOwner, { playButtonState ->
            if (playButtonState == "stop") {
                startPlaying()
            } else stopPlaying()
        })

        // spinning active
        viewModel.spinning.observe(viewLifecycleOwner, {spinning ->
            if (spinning) {
                startSpinning()
            } else stopSpinning()
        })

        // -----------------------------------------------------------------------------------------

        // setup Media Player
        viewModel.setMediaPlayer()

        // perform initial internet check
        initialInternetCheck()

        return binding.root
    }

    // -----------------------------------------------------------------------------------------
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
    private fun initialInternetCheck() {
        // check internet
        val isInternet = context?.let { Utils.isOnline(it) } == true
        Log.i("TitleFragment", "internet check = $isInternet")
        // download xspf file
        if (isInternet) {
            context?.let { viewModel.downloadXSPF(it) }
        }
    }

    // -----------------------------------------------------------------------------------------
    /**
     * Play button was pressed and radio should start playing
     */
    private fun startPlaying() {
        initialInternetCheck()
        viewModel.initializeStream() // start streaming
        // change main button icon
        binding.playButton.setImageResource(R.drawable.stop_button_white)
        // make text views related to streamed content visible
        binding.nowPlaying.visibility = View.VISIBLE
        binding.song.visibility = View.VISIBLE
        binding.bandzoneBanner.visibility = View.VISIBLE
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

    // -----------------------------------------------------------------------------------------
    /**
     * Control circular progress bar behaviour
     */
    private fun startSpinning() {
        binding.playButton.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
    }
    private fun stopSpinning() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.playButton.visibility = View.VISIBLE
    }
}

