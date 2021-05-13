package cz.sumys.rdiosum.fragments

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val binding = DataBindingUtil.inflate<FragmentNewsBinding>(inflater,
            R.layout.fragment_news, container, false)

        // -----------------------------------------------------------------------------------------
        // setup webView content

        val progressIcon = binding.progressBar
        val webView: WebView = binding.webView

        // add progress bar when page is loading
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressIcon.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressIcon.visibility = View.GONE
            }
        }

        webView.loadUrl(URL_FACEBOOK)
        webView.isVerticalScrollBarEnabled = true

        // -----------------------------------------------------------------------------------------

        return binding.root
    }

    companion object {
        private const val URL_FACEBOOK = "https://www.facebook.com/RadioSumys/posts"
    }
}