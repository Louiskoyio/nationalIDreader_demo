package com.louiskoyio.nationalidreader.database;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.louiskoyio.nationalidreader.models.Profile;


@Database(entities = {
        Profile.class}, version = 8)


public abstract class LocalDatabase extends RoomDatabase {

    public static final String DB_NAME = "local_database";

    private static LocalDatabase instance;

    public static synchronized LocalDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), LocalDatabase.class, DB_NAME).allowMainThreadQueries().fallbackToDestructiveMigration().build();
        }
        return instance;
    }

    public abstract DatabaseService databaseService();
}
