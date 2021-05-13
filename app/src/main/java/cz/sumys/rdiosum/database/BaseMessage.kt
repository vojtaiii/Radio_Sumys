package cz.sumys.rdiosum.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "base_messages")
data class BaseMessage(
    @PrimaryKey(autoGenerate = false)
    var messageId: Long = 0L,

    @ColumnInfo(name = "message_text")
    var messageText: String = "Ahoj",

    @ColumnInfo(name = "message_sender")
    var messageSender: String = "Vojta",

    @ColumnInfo(name = "message_timestamp")
    var messageTimestamp: Long = 0L
)