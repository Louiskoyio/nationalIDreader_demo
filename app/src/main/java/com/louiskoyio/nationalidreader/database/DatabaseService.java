package com.louiskoyio.nationalidreader.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.louiskoyio.nationalidreader.models.Profile;

import java.util.List;

@Dao
public interface DatabaseService {
    @Insert
    void saveProfile(Profile profile);

    @Update
    void updateProfile(Profile profile);

    @Query("Select * from profiles")
    List<Profile> getAllProfiles();

    @Query("Select * from profiles where id=:id")
    Profile getProfile(int id);
}
