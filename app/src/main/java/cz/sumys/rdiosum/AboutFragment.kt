package cz.sumys.rdiosum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import cz.sumys.rdiosum.databinding.FragmentAboutBinding


class AboutFragment : Fragment() {

    private lateinit var viewModel: AboutViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val binding = DataBindingUtil.inflate<FragmentAboutBinding>(inflater,
            R.layout.fragment_about, container, false)

        // initialize the view model
        viewModel = ViewModelProvider(this).get(AboutViewModel::class.java)

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
            context?.let { it1 -> viewModel.burp(it1) }
        }

        // -----------------------------------------------------------------------------------------

        // setup media player
        viewModel.setMediaPlayer()

        return binding.root
    }

    // ---------------------------------------------------------------------------------------------

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