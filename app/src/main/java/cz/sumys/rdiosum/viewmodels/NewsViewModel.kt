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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class NewsViewModel(val database: NewsDatabaseDao, application: Application)
    : AndroidViewModel(application) {

    val log: Logger = LoggerFactory.getLogger(NewsViewModel::class.java)
    val news = database.getAllMessages() // gets all the news from database

    //----------------------------------------------------------------------------------------------

    // live data triggering and shutting spinner
    private val _spinning = MutableLiveData<Boolean>()
    val spinning: LiveData<Boolean>
        get() = _spinning

    // tracks last message id for subsequent updates
    var lastMsgId = ""

    //----------------------------------------------------------------------------------------------

    /**
     * Invoke the hear() suspend function and wait for its result, store response
     */
    fun hearFB() {
        viewModelScope.launch(Dispatchers.IO) {
            _spinning.postValue(true)

            val response = hear() // the server side
            parseAndStoreResponse(response.toString()) // parse nad store the received response
            //_finishedResponse.postValue(true)

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
        if (response == "") return

        // assign a reader and perform checking
        val reader: JSONObject
        try {
            reader = JSONObject(response)
        } catch (e: Exception) {
            log.error("Response is probably different then expected, ${e.printStackTrace()}")
            return
        }

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

    /**
     * Send POST data to Facebook graph API, wait for and store the result
     */
    private suspend fun hear() = withContext(Dispatchers.IO) {
        // open a connection to the web script URL
        val url = URL(URL_GRAPH_PAGE_FEED)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

        // set the request method to POST
        connection.requestMethod = "GET"

        // set the request property to JSON
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        // set the response property to JSON
        connection.setRequestProperty("Accept", "application/json")

        // read the server`s response
        try {
            val response = StringBuilder()
            BufferedReader(
                InputStreamReader(connection.inputStream, "utf-8")
            ).use { br ->
                var responseLine: String?
                while (br.readLine().also { responseLine = it } != null) {
                    response.append(responseLine!!.trim { it <= ' ' })
                }
                log.debug("RESPONSE: $response")
            }
            return@withContext response
        } catch (e:Exception) {
            log.error("Failed to access a fb graph server response, ${e.printStackTrace()}")
            return@withContext ""
        }
    }

    //----------------------------------------------------------------------------------------------

    companion object {
        private const val ACCESS_TOKEN = "EAAHPebhRLq0BAObVWqxHKQ2l8PUrzMshXpGly7UL7tutpU9dFIc0wZBJWOC2o5Oi8Ra9VajuM8gL9vT0gEvBfHcpezoMXZCA2ZBTCAakkpeyktNu97JE9UQrNWT7jf7MDaoUgVjAsQnhMp4iV6Xeg1Vus5BhyAzGH6SxzvLEWVPmWq2MLZCs"
        private const val URL_GRAPH_PAGE_FEED = "https://graph.facebook.com/102046414836971/feed?fields=created_time,message,full_picture,access_token&access_token=$ACCESS_TOKEN"
    }
    }