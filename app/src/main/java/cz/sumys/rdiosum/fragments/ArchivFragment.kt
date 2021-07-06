package cz.sumys.rdiosum.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.adapters.ArchivEpisodesAdapter
import cz.sumys.rdiosum.adapters.ListItemListener
import cz.sumys.rdiosum.database.ArchivDatabase
import cz.sumys.rdiosum.databinding.FragmentArchivBinding
import cz.sumys.rdiosum.factories.ArchivViewModelFactory
import cz.sumys.rdiosum.utilities.Utils
import cz.sumys.rdiosum.viewmodels.ArchivViewModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ArchivFragment : Fragment() {
    private lateinit var binding: FragmentArchivBinding
    lateinit var archivViewModel: ArchivViewModel
    val log: Logger = LoggerFactory.getLogger(ArchivFragment::class.java)

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // binding
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_archiv, container, false
        )

        // -----------------------------------------------------------------------------------------
        // setup viewmodel

        // create viewModel, pass it application and databaseDAO reference
        val napplication = requireNotNull(this.activity).application
        val dataSource = ArchivDatabase.getInstance(napplication).archivDatabaseDao
        val viewModelFactory = ArchivViewModelFactory(dataSource, napplication)
        archivViewModel = ViewModelProvider(this, viewModelFactory).get(ArchivViewModel::class.java)

        // -----------------------------------------------------------------------------------------
        // adapter
        // when item is clicked playback fragment is shown

        val adapter = ArchivEpisodesAdapter(ListItemListener { episodeId ->
            archivViewModel.clickedEpisodeId = episodeId
            PlaybackFragment().show(childFragmentManager, episodeId.toString())
        })
        val manager = LinearLayoutManager(context)
        binding.archivRecycler.adapter = adapter
        binding.archivRecycler.layoutManager = manager
        binding.lifecycleOwner = this

        //------------------------------------------------------------------------------------------

        archivViewModel.selectedSeries.observe(viewLifecycleOwner, {
            archivViewModel.requestedEpisodes()
        })

        // observe the selected database, when something is changed, update the recycler view
        archivViewModel.requestedEpisodes.observe(viewLifecycleOwner, {
            it?.let {
                adapter.data = it
            }
        })

        //------------------------------------------------------------------------------------------

        // observe the spinning live data and show spinner
        archivViewModel.spinning.observe(viewLifecycleOwner, { spinning ->
            if (spinning) {
                binding.spinner.visibility = View.VISIBLE
            } else {
                binding.spinner.visibility = View.INVISIBLE
                archivViewModel.getSeriesNames() // triggers series names procedure
            }
        })

        // observe the series name live variable
        // when changed, update the spinner with series names
        archivViewModel.seriesNames.observe(viewLifecycleOwner, { seriesNames ->
            populateSpinner(seriesNames)
        })

        //------------------------------------------------------------------------------------------

        // when user selects item from spinner, change corresponding live data
        binding.archivSelect.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?,
                                        selectedItemView: View, position: Int, id: Long) {
                archivViewModel.seriesSelected(parentView?.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        //------------------------------------------------------------------------------------------

        // initial database request
        archivViewModel.clearDatabase()
        if (internetCheck()) archivViewModel.hearArchiv() // populate the database

        return binding.root
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

    /**
     * Supplies all series name into the spinner
     */
    private fun populateSpinner(seriesNames: List<String>) {
        // create adapter for the spinner
        val adapter: ArrayAdapter<String>? = context?.let {
            ArrayAdapter<String>(
                    it, android.R.layout.simple_spinner_item, seriesNames
            )
        }
        adapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        val spinner = binding.archivSelect
        spinner.adapter = adapter
    }

}