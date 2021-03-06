package cz.sumys.rdiosum

/**
 * Class that reports on the status of the download.
 */
sealed class DownloadResult {

    object Success : DownloadResult()

    data class Error(val message: String, val cause: Exception? = null) : DownloadResult()
}