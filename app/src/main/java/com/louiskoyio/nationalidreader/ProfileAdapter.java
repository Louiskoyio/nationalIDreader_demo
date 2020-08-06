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
        View view = layoutInflater.inflate(R.layout.save_profile, parent, false);

        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position) {

        Profile currentProfile = profiles.get(position);

        holder.mName.setText(currentProfile.getName());
        holder.mIDno.setText(currentProfile.getId_number());
        String filename = currentProfile.getId_number() +".jpg";

        final File file = new File(context.getExternalFilesDir(null),filename);
        Bitmap faceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        holder.mFace.setImageBitmap(faceBitmap);







    }
    class CustomViewHolder extends RecyclerView.ViewHolder {

        public final View mView;

        TextView mIDno, mName;
        ImageView mFace;

        CustomViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            mName = mView.findViewById(R.id.txtName);
            mIDno = mView.findViewById(R.id.txtIDno);
            mFace = mView.findViewById(R.id.imgFace);
        }
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }
}


