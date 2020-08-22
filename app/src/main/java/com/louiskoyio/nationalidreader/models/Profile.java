package com.louiskoyio.nationalidreader.models;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "profiles"
)
public class Profile {

    @PrimaryKey
    private long id;
    private String name;
    private String id_number;
    private String dob;
    private String sex;
    private String district_of_birth;
    private String place_of_issue;
    private String doi;
    private Boolean hasImage;
    private Boolean synced;

    public Boolean getSynced() {
        return synced;
    }

    public void setSynced(Boolean synced) {
        this.synced = synced;
    }

    public Boolean getHasImage() {
        return hasImage;
    }

    public void setHasImage(Boolean hasImage) {
        this.hasImage = hasImage;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
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

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
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
