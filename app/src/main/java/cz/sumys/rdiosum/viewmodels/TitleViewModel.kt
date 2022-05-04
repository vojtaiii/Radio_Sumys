package cz.sumys.rdiosum.viewmodels

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cz.sumys.rdiosum.BuildConfig
import cz.sumys.rdiosum.activities.MainActivity
import cz.sumys.rdiosum.downloadFile
import cz.sumys.rdiosum.utilities.BackgroundSumysService
import cz.sumys.rdiosum.utilities.DownloadResult
import io.ktor.client.*
import io.ktor.client.engine.android.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


class TitleViewModel: ViewModel() {
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)


    // ---------------------------------------------------------------------------------------------
    // LIVE DATA

    // state of the play button
    // "stop" or "play"
    private val _playButtonState = MutableLiveData<String>()
    val playButtonState: LiveData<String>
        get() = _playButtonState

    // active circular progress bar
    private val _spinning = MutableLiveData<Boolean>()
    val spinning: LiveData<Boolean>
        get() = _spinning

    // info file is downloaded
    private val _downloaded = MutableLiveData<Boolean>()
    val downloaded: LiveData<Boolean>
    get() = _downloaded

    var bandzoneAuthor = "Blbě čumíš"
    var band = "Rádio Sumýš"
    var song = "blbě čumíš"
    var currentListeners = "xxx"
    var playing = false

    // ---------------------------------------------------------------------------------------------

    fun setToPlay() {
        _playButtonState.value = "play"
    }

    fun playButtonPressed(isInternet: Boolean) {
        if (_playButtonState.value == "stop") {
            _playButtonState.value = "play"
        } else if (isInternet) _playButtonState.value = "stop"
    }

    fun spinningDone() {
        _spinning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        log.debug("TitleViewModel cleared")
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Asks [BackgroundSumysService] if it is running and return the situation
     */
    fun sumysServiceRunning(): Boolean {
        log.debug("SERVICE_RUNNING = ${BackgroundSumysService.SERVICE_RUNNING}")
        return try {
            BackgroundSumysService.SERVICE_RUNNING
        } catch (e: Exception) {
            log.debug("Failed to contact BackgroundSumysService if its running")
            false
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Periodically send data (band, song) to sumys service
     */
    fun sendDataToSumysService(context: Context) {
        if (playing) {
            val sumysIntent = Intent(context, BackgroundSumysService::class.java)
            sumysIntent.putExtra("band", band)
            sumysIntent.putExtra("song", song)
            // start the service (the execution depends on API version)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(sumysIntent)
            } else context.startService(sumysIntent)
        }
    }


    /**
     * Start streaming
     */
    fun initializeStream(context: Context) {
        _spinning.value = true
        try {
            playing = true
            val sumysIntent = Intent(context, BackgroundSumysService::class.java)
            // start the service (the execution depends on API version)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(sumysIntent)
            } else context.startService(sumysIntent)

            log.debug("Send intent to Sumys background stream")
        } catch (e: Exception) {
            log.error("Failed to send intent to Sumys background stream, ${e.message}")
        }
    }


    /**
     * Stop streaming.
     */
    fun stopStreaming(context: Context) {
        try {
            playing = false
            val sumysIntent = Intent(context, BackgroundSumysService::class.java)
            context.stopService(sumysIntent)
        } catch (e: Exception) {
            log.error("Failed to stop Sumys background stream, ${e.message}")
        }
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Downloading the file.
     * Open the output stream to the URI given and dispatches the download file coroutine.
     */
    fun downloadXSPF(context: Context) {
        log.debug("Downloading XSPF file...")

        val url = INFO_URL
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "sumys_info.xspf"
        val file = File(folder, fileName)
        val uri = context.let {
            FileProvider.getUriForFile(it, "${BuildConfig.APPLICATION_ID}.provider", file)
        }

        val ktor = HttpClient(Android)

        context.contentResolver.openOutputStream(uri)?.let { outputStream ->
            CoroutineScope(Dispatchers.IO).launch {
                ktor.downloadFile(outputStream, url).collect {
                    withContext(Dispatchers.Main) {
                        when (it) {
                            is DownloadResult.Success -> {
                                parseXSPF(context)
                                _downloaded.value = true
                            }

                            is DownloadResult.Error -> {
                                log.error("Downloading of .xspf file failed")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse the downloaded .xspf document in a very naive and simple way.
     * Returns a list of strings:
     *  [0] - author
     *  [1] - song title
     *  [2] - number of current listeners
     */
    private fun parseXSPF(context: Context): List<String> {
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "sumys_info.xspf"
        val file = File(folder, fileName)

        // read all the lines in the document
        val lines: List<String>
        try {
            lines= file.readLines()
        } catch (e: Exception) {
            log.error("Error when parsing .xspf file, ${e.printStackTrace()}")
            return mutableListOf("Rádio Sumýš", "Blbě čumíš", "xxx")
        }

        val authorLine: String
        val songTitleLine: String
        val currListenersLine: String
        try {
            authorLine = lines[7]
            songTitleLine = lines[8]
            currListenersLine = lines[13]
        } catch (e: Exception) {
            log.error("Error when reading .xspf lines, ${e.printStackTrace()}")
            return mutableListOf("Rádio Sumýš", "Blbě čumíš", "xxx")
        }

        val author: String = if (authorLine.contains("<creator>")) {
            authorLine.substringAfter("<creator>").substringBefore("</creator>")
        } else "Rádio Sumýš"
        val songTitle: String = if (songTitleLine.contains("<title>")) {
            songTitleLine.substringAfter("<title>").substringBefore("</title>")
        } else "Blbě čumíš"
        val currListeners: String = if (currListenersLine.contains("Current")) {
            currListenersLine.substringAfter("Current Listeners: ")
        } else "xxx"

        log.debug("XSPF parsed, band = $author, author = $song")

        bandzoneAuthor = author
        band = author
        song = songTitle
        currentListeners = currListeners

        return mutableListOf(author, songTitle, currListeners)
    }

    // ---------------------------------------------------------------------------------------------
    companion object {
        const val STREAM_URL = "https://stream.sumys.cz/sumys-ogg"
        private const val INFO_URL = "https://stream.sumys.cz/sumys-ogg.xspf"
    }
}