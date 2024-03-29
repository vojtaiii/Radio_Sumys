package cz.sumys.rdiosum

import cz.sumys.rdiosum.utilities.DownloadResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.OutputStream

// Methods for file download

/**
 * Creates a coroutine that takes an output stream and URL. Once finished, the data is written to
 * the output stream, and success is returned. Otherwise, there was some failure.
 */
suspend fun HttpClient.downloadFile(file: OutputStream, url: String): Flow<DownloadResult> {
    return flow {
        try {
            val response = call {
                url(url)
                method = HttpMethod.Get
            }.response

            val data = ByteArray(response.contentLength()!!.toInt())
            var offset = 0

            do {
                val currentRead = response.content.readAvailable(data, offset, data.size)
                offset += currentRead
            } while (currentRead > 0)

            response.close()

            if (response.status.isSuccess()) {
                withContext(Dispatchers.IO) {
                    file.write(data)
                }
                emit(DownloadResult.Success)
            } else {
                emit(DownloadResult.Error("File not downloaded"))
            }
        } catch (e: TimeoutCancellationException) {
            emit(DownloadResult.Error("Connection timed out", e))
        } catch (t: Throwable) {
            emit(DownloadResult.Error("Failed to connect"))
        }
    }
}