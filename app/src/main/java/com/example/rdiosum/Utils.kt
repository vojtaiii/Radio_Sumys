package com.example.rdiosum

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.*

object Utils {

    /**
     * Checks whether internet connection is available
     * Source:
     * https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out
     */
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a temporary file which which Media Player can read.
     * For .ogg files only (can be changed).
     */
    fun takeInputStream(stream: InputStream): FileInputStream? {
        try {
            val convertedFile = File.createTempFile("convertedFile", ".ogg", null)
            Log.i("Utility", "Successful file and folder creation.")
            val out = FileOutputStream(convertedFile)
            Log.i("Utility", "Successful set as outputStream")

            // rewrite the file
            val buffer = ByteArray(16384)
            var length = 0
            while (stream.read(buffer).also { length = it } != -1) {
                out.write(buffer, 0, length)
            }

            //stream.read(buffer);
            Log.i("Utility", "Buffer is filled")
            out.close()

            return FileInputStream(convertedFile)

        } catch (e: Exception) {
            Log.e("Utility", "Error when creating temp file, exception = ${e.message}")
            return null
        }
    }

}