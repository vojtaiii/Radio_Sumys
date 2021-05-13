package cz.sumys.rdiosum.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.sumys.rdiosum.database.SumysDatabaseDao
import cz.sumys.rdiosum.viewmodels.ChatViewModel

/**
 * This is pretty much boiler plate code for a ViewModel Factory. CallsViewModelFactory
 *
 * Provides the SleepDatabaseDao and context to the ViewModel.
 */
class ChatViewModelFactory(
    private val dataSource: SumysDatabaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}