package cz.sumys.rdiosum.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.sumys.rdiosum.database.ArchivDatabaseDao;
import cz.sumys.rdiosum.viewmodels.ArchivViewModel


/**
 * This is pretty much boiler plate code for a ViewModel Factory. CallsViewModelFactory
 *
 * Provides the SleepDatabaseDao and context to the ViewModel.
 */
class ArchivViewModelFactory(
        private val dataSource:ArchivDatabaseDao,
        private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchivViewModel::class.java)) {
            return ArchivViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}