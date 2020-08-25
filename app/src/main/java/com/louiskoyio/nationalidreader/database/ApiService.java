package com.louiskoyio.nationalidreader.database;


import com.louiskoyio.nationalidreader.models.ProfileApi;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("/employee/new/")
    Call<ProfileApi> addProfile(@Body ProfileApi profile);

    @GET("employees/{camera_frame}/")
    Call<ProfileApi> getProfile(@Path("camera_frame") int id);

    @GET("employees/")
    Call<List<ProfileApi>> getAllProfiles();

    @PATCH("employees/{camera_frame}/")
    Call<ProfileApi> updateProfile(@Path("camera_frame") int id, @Body ProfileApi profile);

    @DELETE("employees/{camera_frame}/")
    Call<ProfileApi> deleteProfile(@Path("camera_frame") int id, @Body ProfileApi profile);


}
