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

    @POST("/api/late_record/")
    Call<ProfileApi> addProfile(@Body ProfileApi profile);

    @GET("employees/{id}/")
    Call<ProfileApi> getProfile(@Path("id") int id);

    @GET("employees/")
    Call<List<ProfileApi>> getAllProfiles();

    @PATCH("employees/{id}/")
    Call<ProfileApi> updateProfile(@Path("id") int id, @Body ProfileApi profile);

    @DELETE("employees/{id}/")
    Call<ProfileApi> deleteProfile(@Path("id") int id, @Body ProfileApi profile);


}
