package cz.sumys.rdiosum.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BaseMessage::class], version = 1, exportSchema = false)
abstract class SumysDatabase : RoomDatabase() {

    abstract val sumysDatabaseDao: SumysDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: SumysDatabase? = null

        fun getInstance(context: Context): SumysDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        SumysDatabase::class.java,
                        "base_messages_database"
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