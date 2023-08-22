package com.solvek.gardenmonitor.bl.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    version = 1,
    entities = [Point::class]
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun getCalibrationDao(): CalibrationDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "gmonitor"
        ).build()
    }
}