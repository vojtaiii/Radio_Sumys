package cz.sumys.rdiosum.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SumysDatabaseDao {

    @Insert
    suspend fun insert(BaseMessage: BaseMessage): Long

    @Update
    suspend fun update(BaseMessage: BaseMessage)

    @Query("SELECT * from base_messages WHERE messageId = :key")
    suspend fun get(key: Long): BaseMessage?

    @Query("DELETE FROM base_messages")
    suspend fun clear()

    @Query("DELETE FROM base_messages WHERE messageId = :key")
    suspend fun deleteMessage(key: Long)

    @Query("SELECT * FROM base_messages ORDER BY messageId DESC LIMIT 1")
    suspend fun getMessage(): BaseMessage?

    @Query("SELECT * FROM base_messages ORDER BY messageId ASC")
    fun getAllMessages(): LiveData<List<BaseMessage>>
}