package cz.sumys.rdiosum.database;

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_messages")
data class NewsMessage(
        @PrimaryKey(autoGenerate = false)
        var newsId: Long = 0L,

        @ColumnInfo(name = "news_text")
        var newsText: String = "Ahoj",

        @ColumnInfo(name = "news_image")
        var newsImage: String = "Picture",

        @ColumnInfo(name = "news_timestamp")
        var newsTimestamp: Long = 0L
)
