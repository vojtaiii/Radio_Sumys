package com.example.rdiosum

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.engine.android.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Exception


class TitleViewModel: ViewModel() {

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
    fun setMediaPlayer() {
        player.setOnCompletionListener {
            //stopPlaying()
        }
        player.setOnPreparedListener {
            Log.i("TitleViewModel", "Player prepared for streaming")
            _spinning.value = false
            player.start()
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
            Log.e("TitleViewModel", "Media player initialization failed, ${e.printStackTrace()}")
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
            Log.e("TitleViewModel", "Failed to stop the stream, ${e.printStackTrace()}")
        }
    }

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
        Log.i("TitleViewModel", "URI is $uri")
        Log.i("TitleViewModel", "File: $file")

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
                                Log.e("TitleViewModel", "Downloading of .xspf file failed")
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
            Log.e("TitleViewModel", "Error when parsing .xspf file, ${e.printStackTrace()}")
            return mutableListOf("Rádio Sumýš", "Blbě čumíš", "xxx")
        }

        var authorLine: String = ""
        var songTitleLine: String = ""
        var currListenersLine: String = ""
        try {
            authorLine = lines[7]
            songTitleLine = lines[8]
            currListenersLine = lines[13]
        } catch (e: Exception) {
            Log.e("TitleViewModel", "Error when reading .xspf lines, ${e.printStackTrace()}")
            return mutableListOf("Rádio Sumýš", "Blbě čumíš", "xxx")
        }


        val author: String = authorLine.substringAfter("<creator>").substringBefore("</creator>")
        val songTitle: String = songTitleLine.substringAfter("<title>").substringBefore("</title>")
        val currListeners: String = currListenersLine.substringAfter("Current Listeners: ")

        bandzoneAuthor = author

        return mutableListOf(author, songTitle, currListeners)
    }


    companion object {
        private const val STREAM_URL = "https://stream.sumys.cz/sumys-ogg"
        private const val INFO_URL = "https://stream.sumys.cz/sumys-ogg.xspf"
    }
}