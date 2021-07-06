package cz.sumys.rdiosum.database;

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archiv_episodes")
data class ArchivEpisode(
        @PrimaryKey(autoGenerate = false)
        var episodeId: Long = 0L,

        @ColumnInfo(name = "episode_name")
        var episodeName: String = "Patrik nový (Zaseklý Stroje)",

        @ColumnInfo(name = "series_name")
        var seriesName: String = "S hvězdou u pivka",

        @ColumnInfo(name = "episode_number")
        var episodeNumber: Long = 0L
)