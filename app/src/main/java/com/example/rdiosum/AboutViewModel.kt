package com.example.rdiosum

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import java.io.FileInputStream
import java.lang.Exception

class AboutViewModel: ViewModel() {

    var path: FileInputStream? = null

    /**
     *  Set up Media Player - miscellaneous
     */
    private val player: MediaPlayer = MediaPlayer()
    fun setMediaPlayer() {
        player.setOnCompletionListener {
            stopBurp()
        }
        player.setOnPreparedListener {
            player.start()
        }
    }

    /**
     * Start playing the audio file
     */
    fun burp(context: Context) {
        Log.i("AboutViewModel", "Burp")
        try {
            path = Utils.takeInputStream(context.resources.openRawResource(R.raw.burp))
            val fd = path?.fd
            player.setDataSource(fd)
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("AboutViewModel", "Error when setting up the audio file")
        }
    }

    /**
     * Stop playing the audio file
     */
    private fun stopBurp() {
        try {
            player.pause()
            player.reset()
            path?.close()
        } catch (e: Exception) {
            Log.e("AboutViewModel", "Error when stopping audio file")
        }
    }
}