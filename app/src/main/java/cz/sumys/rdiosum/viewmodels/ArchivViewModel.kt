package cz.sumys.rdiosum.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.sumys.rdiosum.database.ArchivDatabaseDao
import cz.sumys.rdiosum.database.ArchivEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class ArchivViewModel(val database: ArchivDatabaseDao, application: Application)
    : AndroidViewModel(application) {

    val log: Logger = LoggerFactory.getLogger(ArchivViewModel::class.java)

    //----------------------------------------------------------------------------------------------

    // live data tracking selected series
    private val _selectedSeries = MutableLiveData<String>("")
    val selectedSeries: LiveData<String>
        get() = _selectedSeries

    // live data triggering and shutting spinner
    private val _spinning = MutableLiveData<Boolean>()
    val spinning: LiveData<Boolean>
        get() = _spinning

    // requested episodes
    private val _requestedEpisodes = MutableLiveData<List<ArchivEpisode>>()
    val requestedEpisodes: LiveData<List<ArchivEpisode>>
        get() = _requestedEpisodes

    // clicked episode Id
    var clickedEpisodeId = 0L

    //----------------------------------------------------------------------------------------------

    /**
     * User selected series from spinner
     */
    fun seriesSelected(series: String) {
        _selectedSeries.value = series
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Retrieve episodes from requested series
     */
    fun requestedEpisodes() {
        viewModelScope.launch {
            val selectedEpisodes = _selectedSeries.value?.let { getSelectedEpisodesFromDatabase(it) }
            if (selectedEpisodes != null) {
                _requestedEpisodes.value = selectedEpisodes
            }
        }
    }

    /**
     * Suspend function for reading the database
     */
    private suspend fun getSelectedEpisodesFromDatabase(series: String) = withContext(Dispatchers.IO) {
        return@withContext database.getAllEpisodesFromSeries(series)
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Invoke the hear() suspend function and wait for its result, store response
     */
    fun hearArchiv() {
        viewModelScope.launch(Dispatchers.IO) {
            _spinning.postValue(true)

            val response = hear() // the server side
            parseAndStoreResponse(response.toString()) // parse nad store the received response

        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Erase the archiv database
     */
    fun clearDatabase() {
        fun eraseDatabase() {
            viewModelScope.launch {
                database.clear()
            }
        }
        eraseDatabase()
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Returns all the series names as live data
     */
    var seriesNames = MutableLiveData<List<String>>()
    fun getSeriesNames() {
        viewModelScope.launch {

            val allEpisodes = getEpisodesFromDatabase()

            if (allEpisodes?.size == null) {
                seriesNames.postValue(listOf("zatim nic"))
                return@launch
            }

            log.debug("Number of episodes is ${allEpisodes?.size}")

            // array to store the names
            val seriesNameArray: MutableList<String> = ArrayList()
            for (episode in allEpisodes!!) { // iterate through the episodes
                seriesNameArray.add(episode.seriesName)
            }
            seriesNames.postValue(seriesNameArray.distinct())
        }
    }

    /**
     * Suspend function for reading the database
     */
    private suspend fun getEpisodesFromDatabase() = withContext(Dispatchers.IO) {
        return@withContext database.getAllEpisodes()
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Parse the received JSON response and store it in the database
     */
    private fun parseAndStoreResponse(response: String) {
        if (response == "") return

        // assign a reader and perform checking
        val reader: JSONArray
        try {
            reader = JSONArray(response)
        } catch (e: Exception) {
            log.error("Response is probably different then expected, ${e.printStackTrace()}")
            _spinning.postValue(false)
            return
        }

        // read single news information and store it in arrays
        val episodesNumber = reader.length()
        if (episodesNumber == 0) return
        val episodeIdArray: MutableList<Long> = ArrayList()
        val episodeNameArray: MutableList<String> = ArrayList()
        val episodeNumberArray: MutableList<Long> = ArrayList()
        val seriesNameArray: MutableList<String> = ArrayList()
        for (i in 0 until episodesNumber) {
            val oneObject = reader.getJSONObject(i)
            // check if "id" field is present
            if (oneObject.has("id_shp")) {
                episodeIdArray.add(oneObject.getString("id_shp").toLong())
            } else {
                episodeIdArray.add(0L)
            }
            // check if "episode_name" field is present
            if (oneObject.has("episode_name")) {
                episodeNameArray.add(oneObject.getString("episode_name"))
            } else {
                episodeNameArray.add("")
            }
            // check if "episode_no" field is present
            if (oneObject.has("episode_no")) {
                episodeNumberArray.add(oneObject.getString("episode_no").toLong())
            } else {
               episodeNumberArray.add(0L)
            }
            // check if "series_name" field is present
            if (oneObject.has("series_name")) {
                seriesNameArray.add(oneObject.getString("series_name"))
            } else {
                seriesNameArray.add("")
            }
        }

        // write the news into the database
        for (i in 0 until episodesNumber) {
            val newEpisode = ArchivEpisode()

            newEpisode.apply {
                episodeId = episodeIdArray[i]
                episodeName = episodeNameArray[i]
                episodeNumber = episodeNumberArray[i]
                seriesName = seriesNameArray[i]
            }

            // insert new recording to database by coroutine
            fun insertToDatabase() {
                viewModelScope.launch {
                    database.insert(newEpisode)
                    if (i == episodesNumber - 1) _spinning.postValue(false)
                }
            }
            insertToDatabase()
        }
        log.debug("New episodes written into the database")
    }

    /**
     * Send POST data to sumys archiv api, wait for and store the result
     */
    private suspend fun hear() = withContext(Dispatchers.IO) {
        // open a connection to the web script URL
        val url = URL(ARCHIV_API_URL)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

        // set the request method to POST
        connection.requestMethod = "POST"

        // set the request property to JSON
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        // set the response property to JSON
        connection.setRequestProperty("Accept", "application/json")

        // read the server`s response
        try {
            val response = StringBuilder()
            BufferedReader(
                    InputStreamReader(connection.inputStream, "utf-8")
            ).use { br ->
                var responseLine: String?
                while (br.readLine().also { responseLine = it } != null) {
                    response.append(responseLine!!.trim { it <= ' ' })
                }
                log.debug("RESPONSE: $response")
            }
            return@withContext response
        } catch (e: Exception) {
            log.error("Failed to access a archiv server response, ${e.printStackTrace()}")
            _spinning.postValue(false)
            return@withContext ""
        }
    }

    //----------------------------------------------------------------------------------------------

    companion object {
        private const val ARCHIV_API_URL = "https://devel.radio.sumys.cz/archiv/shvezdouupivka/json"
    }
}