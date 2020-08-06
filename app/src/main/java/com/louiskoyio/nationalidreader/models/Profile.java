package com.louiskoyio.nationalidreader.models;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "profiles"
)
public class Profile {

    @PrimaryKey
    private int id;
    private String name;
    private String id_number;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId_number() {
        return id_number;
    }

    public void setId_number(String id_number) {
        this.id_number = id_number;
    }
}
