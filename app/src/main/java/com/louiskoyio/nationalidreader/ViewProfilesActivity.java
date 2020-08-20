package com.louiskoyio.nationalidreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.louiskoyio.nationalidreader.database.LocalDatabase;
import com.louiskoyio.nationalidreader.models.Profile;

import java.util.Collections;
import java.util.List;

public class ViewProfilesActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private LocalDatabase localDatabase;
    private ProfileAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profiles);

        localDatabase = LocalDatabase.getInstance(ViewProfilesActivity.this);
        mRecyclerView = findViewById(R.id.recycler_view);

        List<Profile> profiles = localDatabase.databaseService().getAllProfiles();

        fetchProfiles(profiles);
    }


    public void fetchProfiles(List<Profile> profiles) {

        mRecyclerView.setLayoutManager(new LinearLayoutManager(ViewProfilesActivity.this));
        mRecyclerView.setHasFixedSize(true);

        adapter = new ProfileAdapter(profiles, ViewProfilesActivity.this);
        adapter.notifyDataSetChanged();
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ViewProfilesActivity.this, MainActivity.class));
        // optional depending on your needs super.onBackPressed();
    }
}