package cz.sumys.rdiosum

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.PowerManager
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.engine.android.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.stream.Stream


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
    var playing = false

    // ---------------------------------------------------------------------------------------------

    fun playButtonPressed(isInternet: Boolean) {
        if (_playButtonState.value == "stop" && isInternet) {
            _playButtonState.value = "play"
        } else _playButtonState.value = "stop"
    }

    fun spinningDone() {
        _spinning.value = false
    }

    /**
     * Start streaming
     */
    fun initializeStream(context: Context) {
        _spinning.value = true
        try {

            val sumysIntent = Intent(context, BackgroundSumysService::class.java)
            // start the service, for API > 26 we can promise its foreground
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(sumysIntent)
            } else context.startService(sumysIntent)

            log.debug("Send intent to Sumys background stream with foreground intentions")
        } catch (e: Exception) {
            log.error("Failed to send intent to Sumys background stream, ${e.message}")
        }
    }


    /**
     * Stop streaming.
     */
    fun stopStreaming(context: Context) {
        try {
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
    fun parseXSPF(context: Context): List<String> {
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

        val author: String
        val songTitle: String
        val currListeners: String

        author = if (authorLine.contains("<creator>")) {
            authorLine.substringAfter("<creator>").substringBefore("</creator>")
        } else "Rádio Sumýš"
        songTitle = if (songTitleLine.contains("<title>")) {
            songTitleLine.substringAfter("<title>").substringBefore("</title>")
        } else "Blbě čumíš"
        currListeners = if (currListenersLine.contains("Current")) {
            currListenersLine.substringAfter("Current Listeners: ")
        } else "xxx"

        bandzoneAuthor = author

        return mutableListOf(author, songTitle, currListeners)
    }

    // ---------------------------------------------------------------------------------------------
    companion object {
        const val STREAM_URL = "https://stream.sumys.cz/sumys-ogg"
        private const val INFO_URL = "https://stream.sumys.cz/sumys-ogg.xspf"
    }
}