package cz.sumys.rdiosum.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArchivEpisode::class], version = 1, exportSchema = false)
abstract class ArchivDatabase : RoomDatabase() {

    abstract val archivDatabaseDao: ArchivDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: ArchivDatabase? = null

        fun getInstance(context: Context): ArchivDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            ArchivDatabase::class.java,
                            "archiv_episodes_database"
                    )
                            .fallbackToDestructiveMigration()
                            .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}