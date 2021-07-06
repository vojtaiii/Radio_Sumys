package cz.sumys.rdiosum.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ArchivDatabaseDao {

    @Insert
    suspend fun insert(ArchivEpisode: ArchivEpisode): Long

    @Update
    suspend fun update(ArchivEpisode: ArchivEpisode)

    @Query("SELECT * from archiv_episodes WHERE episodeId = :key")
    suspend fun get(key: Long): ArchivEpisode?

    @Query("DELETE FROM archiv_episodes")
    suspend fun clear()

    @Query("DELETE FROM archiv_episodes WHERE episodeId = :key")
    suspend fun deleteEpisode(key: Long)

    @Query("SELECT * FROM archiv_episodes ORDER BY episodeId DESC LIMIT 1")
    suspend fun getEpisode(): ArchivEpisode?

    @Query("SELECT * FROM archiv_episodes ORDER BY episodeId ASC")
    suspend fun getAllEpisodes(): List<ArchivEpisode>?

    @Query("SELECT * FROM archiv_episodes WHERE series_name = :seriesName ORDER BY episodeId ASC")
    suspend fun getAllEpisodesFromSeries(seriesName: String): List<ArchivEpisode>
}