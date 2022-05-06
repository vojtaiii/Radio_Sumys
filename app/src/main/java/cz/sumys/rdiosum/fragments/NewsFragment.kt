package cz.sumys.rdiosum.fragments

import cz.sumys.rdiosum.utilities.Utils
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.adapters.NewsListAdapter
import cz.sumys.rdiosum.database.NewsDatabase
import cz.sumys.rdiosum.databinding.FragmentNewsBinding
import cz.sumys.rdiosum.factories.NewsViewModelFactory
import cz.sumys.rdiosum.viewmodels.NewsViewModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NewsFragment : Fragment() {
    val log: Logger = LoggerFactory.getLogger(NewsFragment::class.java)
    lateinit var newsViewModel: NewsViewModel
    private lateinit var binding: FragmentNewsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_news, container, false)

        // notice about facebook connection
        val facebookNotice = binding.facebookNotice

        // -----------------------------------------------------------------------------------------
        // setup viewmodel

        // create viewModel, pass it application and databaseDAO reference
        val napplication = requireNotNull(this.activity).application
        val dataSource = NewsDatabase.getInstance(napplication).newsDatabaseDao
        val viewModelFactory = NewsViewModelFactory(dataSource, napplication)
        newsViewModel = ViewModelProvider(this, viewModelFactory).get(NewsViewModel::class.java)

        // -----------------------------------------------------------------------------------------
        // adapter

        val adapter = NewsListAdapter()
        val manager = LinearLayoutManager(context)
        binding.newsRecycler.adapter = adapter
        binding.newsRecycler.layoutManager = manager
        binding.lifecycleOwner = this

        //------------------------------------------------------------------------------------------
        // initial database request

        // erase the database when fragment is opened
        newsViewModel.clearDatabase()
        // this scenario is activated if the token is already retrieved but the fragment is reopened
        if (AccessToken.getCurrentAccessToken() != null) {
            log.debug("FB login successful, access token = ${AccessToken.getCurrentAccessToken()}")
            log.debug("Starting to access FB page feed...")
            facebookNotice.isVisible = false
            newsViewModel.hearFB()
        }

        //------------------------------------------------------------------------------------------
        // FB login
        val callbackManager = CallbackManager.Factory.create()
        val loginButton = binding.loginButton
        //login callback
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                facebookNotice.isVisible = false
                if (internetCheck()) newsViewModel.hearFB() // start the retrieval process
            }

            override fun onCancel() {
                log.debug("FB login cancelled")
                facebookNotice.isVisible = true
            }

            override fun onError(error: FacebookException) {
                log.debug("FB login error, $error")
                facebookNotice.isVisible = true
            }
        })

        //------------------------------------------------------------------------------------------

        // observe the database, when something is changed, update the recycler view
        newsViewModel.news.observe(viewLifecycleOwner, {
            it?.let {
                adapter.data = it
            }
        })

        //------------------------------------------------------------------------------------------

        // observe the spinning live data and show spinner
        newsViewModel.spinning.observe(viewLifecycleOwner, { spinning ->
            if (spinning) {
                binding.spinner.visibility = View.VISIBLE
            } else {
                binding.spinner.visibility = View.INVISIBLE
            }
        })

        //------------------------------------------------------------------------------------------

        return binding.root
    }

    /**
     * Performs check whether a internet connection is available.
     */
    private fun internetCheck(): Boolean {
        // check internet
        val isInternet = context?.let { Utils.isOnline(it) } == true
        return if (!isInternet) {
            context?.let { Toast.makeText(it, "Musí ti fungovat internet", Toast.LENGTH_SHORT).show() }
            log.debug("Internet check: false")
            false
        } else {
            log.debug("Internet check: true")
            true
        }
    }

}