package com.louiskoyio.nationalidreader.models;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

public class ProfileApi {

    private long id;
    private String name;
    private String id_number;
    private String date_of_birth;
    private String gender;
    private String district_of_birth;
    private String place_of_issue;
    private String date_of_issue;
    private byte[] img;

    public String getDate_of_birth() {
        return date_of_birth;
    }

    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDate_of_issue() {
        return date_of_issue;
    }

    public void setDate_of_issue(String date_of_issue) {
        this.date_of_issue = date_of_issue;
    }

    public byte[] getImg() {
        return img;
    }

    public void setImg(byte[] img) {
        this.img = img;
    }

    public String getDistrict_of_birth() {
        return district_of_birth;
    }

    public void setDistrict_of_birth(String district_of_birth) {
        this.district_of_birth = district_of_birth;
    }

    public String getPlace_of_issue() {
        return place_of_issue;
    }

    public void setPlace_of_issue(String place_of_issue) {
        this.place_of_issue = place_of_issue;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
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
