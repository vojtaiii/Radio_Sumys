package cz.sumys.rdiosum.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.sumys.rdiosum.database.NewsDatabaseDao
import cz.sumys.rdiosum.database.NewsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import com.facebook.GraphResponse
import com.facebook.AccessToken
import com.facebook.HttpMethod
import com.facebook.GraphRequest


class NewsViewModel(val database: NewsDatabaseDao, application: Application)
    : AndroidViewModel(application) {

    val log: Logger = LoggerFactory.getLogger(NewsViewModel::class.java)
    val news = database.getAllMessages() // gets all the news from database

    //----------------------------------------------------------------------------------------------

    // live data triggering and shutting spinner
    private val _spinning = MutableLiveData<Boolean>()
    val spinning: LiveData<Boolean>
        get() = _spinning

    //----------------------------------------------------------------------------------------------

    /**
     * Invoke the hearFBPageAndStoreResponse() suspend function, wait for the result,
     * store the response.
     */
    fun hearFB() {
        viewModelScope.launch(Dispatchers.IO) {
            _spinning.postValue(true)
             hearFBPageAndStoreResponse()
            _spinning.postValue(false)
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Erase the news database
     */
    fun clearDatabase() {
        fun eraseDatabase() {
            viewModelScope.launch {
                database.clear()
            }
        }
        eraseDatabase()
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Parse the received JSON response and store it in the database
     */
    private fun parseAndStoreResponse(response: String) {
        if (response == "") {
            log.debug("Parsing empty response, exiting...")
            return
        }

        // assign a reader and perform checking
        val reader: JSONObject
        try {
            reader = JSONObject(response)
        } catch (e: Exception) {
            log.error("Response is probably different then expected, ${e.printStackTrace()}")
            return
        }
        log.debug("Processing FB feed response, response = $response")

        // read single news information and store it in arrays
        val messagesArray = reader.getJSONArray("data")
        val messagesNumber = messagesArray.length()
        if (messagesNumber == 0) return
        val msg: MutableList<String> = ArrayList()
        val pic: MutableList<String> = ArrayList()
        val date: MutableList<String> = ArrayList()
        for (i in 0 until messagesNumber) {
            val oneObject = messagesArray.getJSONObject(i)
            // check if "message" field is present
            if (oneObject.has("message")) {
                msg.add(oneObject.getString("message"))
            } else {
                msg.add("")
            }
            // check if picture filed is present
            if (oneObject.has("full_picture")) {
                pic.add(oneObject.getString("full_picture"))
            } else {
                pic.add("")
            }
            date.add(oneObject.getString("created_time"))
        }

        // write the news into the database
        for (i in 0 until messagesNumber) {
            val newNews = NewsMessage()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+0000'", Locale.ROOT)
            var dateMillis = 1621495367000L
            try {
                dateMillis = LocalDateTime.parse(date[i], formatter)
                    .atZone(ZoneId.of("Europe/Prague"))
                    .toOffsetDateTime()
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                log.error("Something went wrong with the date format")
            }

            newNews.apply {
                newsId = i.toLong()
                newsTimestamp = dateMillis
                newsText = msg[i]
                newsImage = pic[i]
            }

            // insert new recording to database by coroutine
            fun insertToDatabase() {
                viewModelScope.launch {
                    database.insert(newNews)
                    log.debug("Inserted: id = ${newNews.newsId}, timestamp = ${newNews.newsTimestamp}")
                }
            }
            insertToDatabase()
        }
        log.debug("New messages written into the database")
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Access the page feed via FB API and store the response.
     */
    private suspend fun hearFBPageAndStoreResponse() = withContext(Dispatchers.IO) {
        /* make the API call */
        GraphRequest(AccessToken.getCurrentAccessToken(),
        "/$PAGE_ID/feed?fields=created_time,message,full_picture",
        null,
        HttpMethod.GET,
            object : GraphRequest.Callback {
                override fun onCompleted(response: GraphResponse) {
                    log.debug("Graph response retrieved, token = ${AccessToken.getCurrentAccessToken()}")
                    parseAndStoreResponse(response.rawResponse.toString())
                }
            }
        ).executeAsync()
    }

    //----------------------------------------------------------------------------------------------

    companion object {
        private const val PAGE_ID = "102046414836971"
        private const val URL_GRAPH_PAGE_FEED = "https://graph.facebook.com/102046414836971/feed?fields=created_time,message,full_picture"
    }
    }