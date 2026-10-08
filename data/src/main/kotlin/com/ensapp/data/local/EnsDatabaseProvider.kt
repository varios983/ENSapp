package com.ensapp.data.local

import android.content.Context
import androidx.room.Room

object EnsDatabaseProvider {
    fun create(context: Context): EnsDatabase =
        Room.databaseBuilder(context, EnsDatabase::class.java, DATABASE_NAME)
            .build()

    private const val DATABASE_NAME = "ensapp.db"
}
