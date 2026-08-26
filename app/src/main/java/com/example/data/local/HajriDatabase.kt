package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayType
import com.example.data.model.SignatureRecord
import com.example.data.model.Worker

class Converters {
    @TypeConverter
    fun fromPayType(value: PayType?): String = value?.name ?: PayType.DAILY.name

    @TypeConverter
    fun toPayType(value: String?): PayType =
        try {
            value?.let { PayType.valueOf(it) } ?: PayType.DAILY
        } catch (e: Exception) {
            PayType.DAILY
        }

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus?): String =
        value?.name ?: AttendanceStatus.PRESENT.name

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus =
        try {
            value?.let { AttendanceStatus.valueOf(it) } ?: AttendanceStatus.PRESENT
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
        }
}

@Database(
    entities = [
        Worker::class,
        Attendance::class,
        SignatureRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HajriDatabase : RoomDatabase() {
    abstract fun hajriDao(): HajriDao

    companion object {
        @Volatile
        private var INSTANCE: HajriDatabase? = null

        fun getDatabase(context: Context): HajriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HajriDatabase::class.java,
                    "hajri_card_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
