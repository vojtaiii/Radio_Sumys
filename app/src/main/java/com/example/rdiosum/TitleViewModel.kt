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

    // ---------------------------------------------------------------------------------------------

    fun playButtonPressed() {
        if (_playButtonState.value == "stop") {
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
                                Toast.makeText(
                                    context,
                                    "File successfully downloaded",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is DownloadResult.Error -> {
                                Toast.makeText(
                                    context,
                                    "Error while downloading file",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }


    companion object {
        private const val STREAM_URL = "https://stream.sumys.cz/sumys-ogg"
        private const val INFO_URL = "https://stream.sumys.cz/sumys-ogg.xspf"
    }
}