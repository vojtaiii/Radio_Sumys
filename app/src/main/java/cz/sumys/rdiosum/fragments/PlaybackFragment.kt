package cz.sumys.rdiosum.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.databinding.FragmentPlaybackBinding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PlaybackFragment: DialogFragment() {
    val log: Logger = LoggerFactory.getLogger(PlaybackFragment::class.java)
    private lateinit var binding: FragmentPlaybackBinding
    private lateinit var playbackPlayer: SimpleExoPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate<FragmentPlaybackBinding>(inflater,
            R.layout.fragment_playback, container, false)

        // the player view instance
        val playbackView : StyledPlayerView = binding.playbackView

        // player attributes
        val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()

        // the player instance
        playbackPlayer = SimpleExoPlayer.Builder(requireContext()).build().apply {
            setAudioAttributes(audioAttributes, true)
        }
        playbackView.player = playbackPlayer

        // apply additional attributes
        playbackView.apply {
            defaultArtwork = ResourcesCompat.getDrawable(resources, R.drawable.sumys_baner, null)
        }

        // the source
        val audioUrl = tag?.let { chooseVideoSource(it) }
        val mediaItem = MediaItem.fromUri(Uri.parse(audioUrl))
        playbackPlayer.setMediaItem(mediaItem)

        // assign listeners
        playbackPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> binding.spinner.visibility = View.VISIBLE
                    else -> binding.spinner.visibility = View.GONE
                }
            }
        })

        // prepare the player for streaming
        playbackPlayer.playWhenReady = true
        playbackPlayer.prepare()

        return binding.root
    }

    private fun chooseVideoSource(tag: String): String {
        return "https://devel.radio.sumys.cz/archiv/shvezdouupivka/play/$tag"
    }

    /**
     * Release the video player in onStop
     */
    override fun onDestroy() {
        playbackPlayer.release()

        super.onDestroy()
    }
}