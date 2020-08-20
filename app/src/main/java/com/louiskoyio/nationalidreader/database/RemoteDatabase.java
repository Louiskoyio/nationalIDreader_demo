package com.louiskoyio.nationalidreader.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.louiskoyio.nationalidreader.models.Profile;

public class RemoteDatabase{

    private String url = "jdbc:postgresql://35.238.251.114:5432/valleygate?user=odoo12&password=Irakiza12";
    private Connection conn;
    private String sql;


    public RemoteDatabase() throws SQLException {
        conn = DriverManager.getConnection(url);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void sendRecord(Profile profile){
        String result = "";

        int id = profile.getId();
        String name = profile.getName();
        String id_number = profile.getId_number();
        String dob = profile.getDob();
        String sex = profile.getSex();
        String district = profile.getDistrict_of_birth();
        String poi = profile.getPlace_of_issue();
        String doi = profile.getDoi();
        Boolean hasImage = profile.getHasImage();

        sql = "INSERT INTO hr_employee (name, id_number, dob, sex, district_of_birth,place_of_issue,district_of_issue,hasImage)" +
                "VALUES ("+ name +", "+ id_number +", "+ dob +", "+ sex +", "+ district +","+ dob +", "+ poi +", "+ district +","+doi+")";




        try {
            DriverManager.setLoginTimeout(5);

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);
            while(rs.next()) {
                result = rs.getString(1);
            }
            rs.close();
            st.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = e.toString();
        }
    }


}