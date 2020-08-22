package com.louiskoyio.nationalidreader.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.louiskoyio.nationalidreader.ViewProfilesActivity;
import com.louiskoyio.nationalidreader.database.ApiService;
import com.louiskoyio.nationalidreader.database.LocalDatabase;
import com.louiskoyio.nationalidreader.database.RetrofitClient;
import com.louiskoyio.nationalidreader.models.Profile;
import com.louiskoyio.nationalidreader.models.ProfileApi;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataSender {

    Context context;
    ApiService apiService;
    Profile profile;
    ProfileApi apiFormatProfile;
    LocalDatabase localDatabase;

    public DataSender(Context context,Profile profile) {
        this.context = context;
        this.profile = profile;
        this.apiService = RetrofitClient.getClient(context).create(ApiService.class);
        this.apiFormatProfile = convertToApiFormat(profile);
        this.localDatabase = LocalDatabase.getInstance(context);
    }

    public void sendProfileToApi(){
        if(!profile.getSynced()) {
            apiService.addProfile(apiFormatProfile).enqueue(new Callback<ProfileApi>() {
                @Override
                public void onResponse(Call<ProfileApi> call, Response<ProfileApi> response) {
                    if (response.isSuccessful()) {
                        profile.setSynced(true);
                        localDatabase.databaseService().updateProfile(profile);
                    }
                }

                @Override
                public void onFailure(Call<ProfileApi> call, Throwable t) {

                }
            });
        }
    }

    public ProfileApi convertToApiFormat(Profile profile){
        ProfileApi apiFormatProfile = new ProfileApi();

        apiFormatProfile.setId_number(profile.getId_number());
        apiFormatProfile.setName(profile.getName());
        apiFormatProfile.setDate_of_birth(profile.getDob());
        apiFormatProfile.setGender(profile.getSex());
        apiFormatProfile.setDistrict_of_birth(profile.getDistrict_of_birth());
        apiFormatProfile.setPlace_of_issue(profile.getPlace_of_issue());
        apiFormatProfile.setDate_of_issue(profile.getDoi());

        if(profile.getHasImage())
            apiFormatProfile.setImg(setImage(profile.getId_number()));


        return apiFormatProfile;

    }

    public byte[] setImage(String idNumber){
        final File file = new File(context.getExternalFilesDir(null),idNumber);
        Bitmap faceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        return BitmapUtils.convertBitmapToNv21Bytes(faceBitmap);

    }

}
