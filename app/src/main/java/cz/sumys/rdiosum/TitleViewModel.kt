package cz.sumys.rdiosum

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.PowerManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
    var playing = false

    // ---------------------------------------------------------------------------------------------

    fun playButtonPressed(isInternet: Boolean) {
        if (_playButtonState.value == "stop" && isInternet) {
            _playButtonState.value = "play"
        } else _playButtonState.value = "stop"
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Prepare the media player to start streaming.
     */
    private val player: MediaPlayer = MediaPlayer()
    fun setMediaPlayer(context: Context) {
        // set wake lock so the phone cpu is still ready while streaming
        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player.setOnCompletionListener {
            //stopPlaying()
        }
        player.setOnPreparedListener {
            log.debug("Player prepared for streaming")
            _spinning.value = false
            player.start()
        }
        player.setOnErrorListener { mp, what, extra ->
            log.debug("Media Player error = WHAT: $what EXTRA: $extra")
            false
        }
        player.setOnBufferingUpdateListener { mp, percent ->
            log.debug("Media Player buffering update, mp: $mp, percent: $percent")
        }
    }

    /**
     * Initialize wifi lock
     */
    fun acquireWifiLock(wifiLock: WifiManager.WifiLock) {
        try {
            wifiLock.acquire()
        } catch (e: Exception) {
            log.error("Failed to acquire wifi lock")
        }
    }
    fun releaseWifiLock(wifiLock: WifiManager.WifiLock) {
        try {
            wifiLock.release()
        } catch (e: Exception) {
            log.error("Failed to release wifi lock")
        }
    }

    /**
     * Manage wake locks (seems they might be automatically released)
     */
    @SuppressLint("WakelockTimeout")
    fun reacquireWakeLock(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"sumys:wakelock" )
        wakeLock?.setReferenceCounted(false) // each wake lock can be released with single release()
        try {
            wakeLock?.acquire()
            log.debug("Wake lock reacquired")
        } catch (e: Exception) {
            log.error("Failed to reacquire wake lock")
        }
    }
    fun releaseWakeLock(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"sumys:wakelock" )
        try {
            wakeLock?.release()
        } catch (e: Exception) {
            log.error("Failed to release wake lock")
        }

    }

    /**
     * Start streaming
     */
    fun initializeStream() {
        try {
            player.setDataSource(STREAM_URL)
            player.prepareAsync()
            _spinning.value = true
        } catch (e: Exception) {
            log.error("Media player initialization failed, ${e.printStackTrace()}")
        }
    }

    /**
     * Stop streaming.
     */
    fun stopStreaming() {
        try {
            player.pause()
            player.reset()
        } catch (e: Exception) {
            log.error("Failed to stop the stream, ${e.printStackTrace()}")
        }
    }

    fun playerTrackInfo() {
        log.info("Player track info retrieved, info = ${player.trackInfo.size}")
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
        private const val STREAM_URL = "https://stream.sumys.cz/sumys-ogg"
        private const val INFO_URL = "https://stream.sumys.cz/sumys-ogg.xspf"
    }
}