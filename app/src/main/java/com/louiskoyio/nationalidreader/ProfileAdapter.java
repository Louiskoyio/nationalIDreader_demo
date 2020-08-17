package com.louiskoyio.nationalidreader;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.louiskoyio.nationalidreader.models.Profile;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.CustomViewHolder>{

    private List<Profile> profiles;
    private Context context;

    public ProfileAdapter(List<Profile> profiles, Context context) {
        this.profiles = profiles;
        this.context = context;
    }
    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.profiles_list, parent, false);

        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position) {

        final Profile currentProfile = profiles.get(position);


        String nameString="";
        String[] nameArr = currentProfile.getName().split(" ");

        for(String name:nameArr){
            nameString = nameString + name + "\n";
        }

        holder.mName.setText(nameString);
        holder.mIDno.setText(currentProfile.getId_number());
        holder.mDob.setText(currentProfile.getDob());
        holder.mDistrict.setText(currentProfile.getDistrict_of_birth());
        holder.mPoi.setText(currentProfile.getPlace_of_issue());
        holder.mDoi.setText(currentProfile.getDoi());
        holder.mGender.setText(currentProfile.getSex());




        String filename = currentProfile.getId_number() +".jpg";

        final File file = new File(context.getExternalFilesDir(null),filename);
        Bitmap faceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        holder.mFace.setImageBitmap(faceBitmap);

        holder.mFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context,ProfileActivity.class);
                context.startActivity(intent.putExtra("id",currentProfile.getId()));
            }
        });


    }
    class CustomViewHolder extends RecyclerView.ViewHolder {

        public final View mView;

        TextView mIDno, mName,mDob,mDistrict,mPoi,mDoi,mGender;
        ImageView mFace;

        CustomViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            mName = mView.findViewById(R.id.name);
            mIDno = mView.findViewById(R.id.idnum);
            mDob = mView.findViewById(R.id.dob);
            mDistrict = mView.findViewById(R.id.district);
            mPoi = mView.findViewById(R.id.poi);
            mDoi = mView.findViewById(R.id.doi);
            mGender = mView.findViewById(R.id.sex);

            mFace = mView.findViewById(R.id.imgFace);
        }
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }
}


