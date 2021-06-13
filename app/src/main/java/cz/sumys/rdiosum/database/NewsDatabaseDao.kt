package cz.sumys.rdiosum.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NewsDatabaseDao {

    @Insert
    suspend fun insert(NewsMessage: NewsMessage): Long

    @Update
    suspend fun update(NewsMessage: NewsMessage)

    @Query("SELECT * from news_messages WHERE newsId = :key")
    suspend fun get(key: Long): NewsMessage?

    @Query("DELETE FROM news_messages")
    suspend fun clear()

    @Query("DELETE FROM news_messages WHERE newsId = :key")
    suspend fun deleteMessage(key: Long)

    @Query("SELECT * FROM news_messages ORDER BY newsId DESC LIMIT 1")
    suspend fun getMessage(): NewsMessage?

    @Query("SELECT * FROM news_messages ORDER BY newsId ASC")
    fun getAllMessages(): LiveData<List<NewsMessage>>
}