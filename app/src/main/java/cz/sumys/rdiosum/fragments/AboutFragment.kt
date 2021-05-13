package cz.sumys.rdiosum.fragments

import android.content.Intent
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.databinding.FragmentAboutBinding


class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val binding = DataBindingUtil.inflate<FragmentAboutBinding>(inflater,
            R.layout.fragment_about, container, false)

        // -----------------------------------------------------------------------------------------
        // LISTENERS

        // facebook
        binding.fb.setOnClickListener {
            createIntent("facebook")
        }

        // instagram
        binding.instagram.setOnClickListener {
            createIntent("instagram")
        }

        // buy me a beer
        binding.beer.setOnClickListener {
            createIntent("beer")
        }

        // miscellaneous
        binding.mara1.setOnClickListener {
           playSound(R.raw.burp)
        }
        binding.sumysSpace.setOnClickListener {
            playSound(R.raw.sumys_cut)
        }

        // -----------------------------------------------------------------------------------------

        return binding.root
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Play a sound using soundPool
     */
    private fun playSound(sound: Int) {
        val soundPool = SoundPool.Builder().build()
        val soundId = soundPool.load(context, sound, 1)
        soundPool.setOnLoadCompleteListener { _, _, _ ->
            soundPool.play(soundId, 1F, 1F, 1, 0, 1F) }
    }

    /**
     * Creates intent with facebook content
     */
    private fun createIntent(param: String) {
        val i = Intent(Intent.ACTION_VIEW)
        val url = when (param) {
            "facebook" -> FACEBOOK_URL
            "instagram" -> INSTAGRAM_URL
            else -> BEER_URL
        }
        i.data = Uri.parse(url)
        try {
            startActivity(i)
        } catch (e: Exception) {
            Log.e("TitleFragment", "Failed to launch intent to info")
        }
    }

    // ---------------------------------------------------------------------------------------------

    companion object {
        private const val FACEBOOK_URL = "https://www.facebook.com/RadioSumys"
        private const val INSTAGRAM_URL = "https://www.instagram.com/radio.sumys/"
        private const val BEER_URL = "https://www.buymeacoffee.com/sumys"
    }

}