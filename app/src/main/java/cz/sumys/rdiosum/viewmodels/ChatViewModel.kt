
package cz.sumys.rdiosum.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.sumys.rdiosum.database.BaseMessage
import cz.sumys.rdiosum.database.SumysDatabaseDao
import kotlinx.coroutines.*
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


class ChatViewModel(val database: SumysDatabaseDao, application: Application)
    : AndroidViewModel(application) {

    val log: Logger = LoggerFactory.getLogger(ChatViewModel::class.java)
    val messages = database.getAllMessages() // gets all the messages from database

    //----------------------------------------------------------------------------------------------

    // live data that triggers event when new responses are stored
    private val _finishedResponse = MutableLiveData<Boolean>()
    val finishedResponse: LiveData<Boolean>
        get() = _finishedResponse

    // live data triggering and shutting spinner
    private val _spinning = MutableLiveData<Boolean>()
    val spinning: LiveData<Boolean>
        get() = _spinning

    // tracks last message id for subsequent updates
    var firstMsgId = ""

    // to prevent double refresh when scrolling recycler
    var timeOfLastRefresh = System.currentTimeMillis()

    //----------------------------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        log.debug("ChatViewModel cleared")
    }

    /**
     * Invoke the hear() suspend function and wait for its result, store response
     */
    fun hearChat(prev: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _spinning.postValue(true)

            val response = hear(prev) // the server side
            parseAndStoreResponse(response.toString()) // what happens here
            if (!prev) _finishedResponse.postValue(true)

            _spinning.postValue(false)
        }
    }

    /**
     * Invoke the mluv() function and write new message to database
     */
    fun mluvChat(nick: String, msg: String, date:String) {
        viewModelScope.launch(Dispatchers.IO) {
            _spinning.postValue(true)

            mluv(nick, msg, date)
            writeNewMessage(nick, msg, date)

            _spinning.postValue(false)
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Erase the messages database
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

    private suspend fun mluv(nick: String, msg: String, date:String) = withContext(Dispatchers.IO) {
        val connection = LIVECHAT_MLUV_URL.openConnection() as HttpURLConnection

        // set the request method to POST
        connection.requestMethod = "POST"

        // set the request property to JSON
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        // set the response property to JSON
        connection.setRequestProperty("Accept", "application/json")

        // to send request content, enable the URLConnection doOutput property to true
        // otherwise, we won't be able to write content to the connection output stream
        connection.doOutput = true

        // INPUT
        val jsonInputString = """{"nick": "$nick", "msg": "$msg", "date": "$date"}"""

        // write the string to connection output stream
        connection.outputStream.use { os ->
            val input = jsonInputString.toByteArray(charset("utf-8"))
            os.write(input, 0, input.size)
        }

        log.debug("Sending message to server")

        // get the input stream to read the response content
        // remember to use try-with-resources to close the response stream automatically
        // read through the whole response content
        val response = StringBuilder()
        BufferedReader(
                InputStreamReader(connection.inputStream, "utf-8")).use { br ->
            var responseLine: String?
            while (br.readLine().also { responseLine = it } != null) {
                response.append(responseLine!!.trim { it <= ' ' })
            }
            log.debug("POST RESPONSE: $response") }
    }

    /**
     * Check nickname
     */
    fun checkNickname(nick: String): Boolean {
        return !(nick.isEmpty() || nick.length > MAX_NICK_LENGTH)
    }
    /**
     * Check message
     */
    fun checkMessage(msg: String):Boolean {
        return msg.isNotEmpty()
    }
    //----------------------------------------------------------------------------------------------

    private fun writeNewMessage(nick: String, msg: String, date:String) {
        val newMessage = BaseMessage()
        newMessage.apply {
            messageId = date.toLong()
            messageText = msg
            messageSender = nick
            messageTimestamp = date.toLong()
        }

        // insert new recording to database by coroutine
        fun insertToDatabase() {
            viewModelScope.launch {
                database.insert(newMessage)
            }
        }
        insertToDatabase()
        log.debug("New massage written in the database")
    }

    /**
     * Parse the received JSON response and store it in the database
     * Also update [firstMsgId]
     */
    private fun parseAndStoreResponse(response: String) {
        val reader = JSONObject(response)

        // read system data
        val system = reader.getJSONObject("system")
        val firstId = system.getString("firstId")
        firstMsgId = firstId

        // read messages and store them in arrays
        val msgsArray = reader.getJSONArray("msgs")
        val msgsNumber = msgsArray.length()
        if (msgsNumber == 0) return
        val msg: MutableList<String> = ArrayList()
        val nick: MutableList<String> = ArrayList()
        val date: MutableList<String> = ArrayList()
        for (i in 0 until msgsNumber) {
            try {
                val oneObject = msgsArray.getJSONObject(i)
                msg.add(oneObject.getString("msg"))
                nick.add(oneObject.getString("nick"))
                date.add(oneObject.getString("date"))
            } catch (e: Exception) {
                log.error("Error parsing JSON file, ${e.printStackTrace()}")
            }
        }

        // write the stored messages into the database
        for (i in 0 until msgsNumber) {
            val newMessage = BaseMessage()
            newMessage.apply {
                messageId = date[i].toLong()
                messageText = msg[i]
                messageSender = nick[i]
                messageTimestamp = date[i].toLong()
            }

            // insert new recording to database by coroutine
            fun insertToDatabase() {
                viewModelScope.launch {
                    database.insert(newMessage)
                }
            }
            insertToDatabase()
        }
        log.debug("New messages written into the database")
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Send POST data to "livechat-slys.py", wait for and store the result
     * [prev] toggles between initial and subsequent request
     */
    private suspend fun hear(prev: Boolean) = withContext(Dispatchers.IO) {
        // open a connection to the web script URL
        val connection: HttpURLConnection = if (prev) {
            LIVECHAT_PREV_URL.openConnection() as HttpURLConnection
        } else {
            LIVECHAT_SLYS_URL.openConnection() as HttpURLConnection
        }

        // set the request method to POST
        connection.requestMethod = "POST"

        // set the request property to JSON
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        // set the response property to JSON
        connection.setRequestProperty("Accept", "application/json")

        // to send request content, enable the URLConnection doOutput property to true
        // otherwise, we won't be able to write content to the connection output stream
        connection.doOutput = true

        // here, pass either empty string for default response or lastMsgId (!)
        val jsonInputString = firstMsgId

        // write the string to connection output stream
        // this HAVE to be done in a coroutine
        connection.outputStream.use { os ->
            val input = jsonInputString.toByteArray(charset("utf-8"))
            os.write(input, 0, input.size)
        }

        // get the input stream to read the response content
        // remember to use try-with-resources to close the response stream automatically
        // read through the whole response content
        val response = StringBuilder()
        BufferedReader(
                InputStreamReader(connection.inputStream, "utf-8")).use { br ->
            var responseLine: String?
            while (br.readLine().also { responseLine = it } != null) {
                response.append(responseLine!!.trim { it <= ' ' })
            }
            log.debug("RESPONSE: $response") }

        return@withContext response
    }


    private companion object {
        private const val MAX_NICK_LENGTH: Int = 12
        private val LIVECHAT_SLYS_URL: URL = URL("https://devel.radio.sumys.cz/scripts/livechat-slys.py")
        private val LIVECHAT_PREV_URL: URL = URL("https://devel.radio.sumys.cz/scripts/livechat-prev.py")
        private val LIVECHAT_MLUV_URL: URL = URL("https://devel.radio.sumys.cz/scripts/livechat-mluv.py")
    }
}
