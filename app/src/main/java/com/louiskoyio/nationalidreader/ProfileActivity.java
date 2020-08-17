package com.louiskoyio.nationalidreader;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.louiskoyio.nationalidreader.database.LocalDatabase;
import com.louiskoyio.nationalidreader.models.Profile;
import com.louiskoyio.nationalidreader.utilities.ImageSaver;

import java.io.File;
import java.io.IOException;

import static com.louiskoyio.nationalidreader.MainActivity.REQUEST_TAKE_PHOTO;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    static final int REQUEST_TAKE_PHOTO = 100;

    private Button home,profiles;
    private ProgressDialog mProgressDialog;
    private ImageView imgFace;
    private Bitmap mBitmap,mFaceBitmap;
    private TextView txtName, txtIdNumber,txtSex,txtDoB,txtDistrict,txtPoI,txtDoI;
    private LocalDatabase localDatabase;
    private String currentPhotoPath;
    Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        localDatabase = LocalDatabase.getInstance(this);

        imgFace = findViewById(R.id.imgFace);

        txtIdNumber = findViewById(R.id.txtIDno);
        txtName = findViewById(R.id.txtName);
        txtSex = findViewById(R.id.txtSex);
        txtDoB = findViewById(R.id.txtDoB);
        txtPoI =  findViewById(R.id.txtPoI);
        txtDoI= findViewById(R.id.txtDateOfIssue);
        txtDistrict = findViewById(R.id.txtDistrict);

        home = findViewById(R.id.button_home);
        profiles = findViewById(R.id.button_profiles);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            }
        });

        profiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, ViewProfilesActivity.class));
            }
        });




        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle!=null){
            int profileID = bundle.getInt("id",0);

            getDetails(profileID);
        }

    }
    private void getDetails(int id){
        profile = localDatabase.databaseService().getProfile(id);


        txtName.setText(profile.getName());
        txtIdNumber.setText(profile.getId_number());
        txtSex.setText(profile.getSex());
        txtDoB.setText(profile.getDob());
        txtDistrict.setText(profile.getDistrict_of_birth());
        txtPoI.setText(profile.getPlace_of_issue());
        txtDoI.setText(profile.getDoi());

        if(profile.getHasImage()) {
            String filename = profile.getId_number() + ".jpg";

            final File file = new File(ProfileActivity.this.getExternalFilesDir(null), filename);
            Bitmap faceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            imgFace.setImageBitmap(faceBitmap);
        }else {
            requestFaceImage();
        }
    }

    private void requestFaceImage(){
        AlertDialog.Builder saveProfileDialogBuilder = new AlertDialog.Builder(ProfileActivity.this);
        saveProfileDialogBuilder.setTitle("Retry face detection");
        saveProfileDialogBuilder.setMessage("No face image was detected for this profile, do you want to try again with another picture?");
        saveProfileDialogBuilder.setCancelable(true);
        saveProfileDialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dispatchTakePictureIntent();
            }
        });

        saveProfileDialogBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
            }
        });
        AlertDialog saveProfileAlert = saveProfileDialogBuilder.create();
        saveProfileAlert.show();

    }


    private File createImageFile() throws IOException {
        // Create an image file name


        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                "temp_image",  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }

    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        mBitmap=getBitmap(f);


        processFace();

    }
    public Bitmap getBitmap(File image) {
        float scaleFactor =
                Math.max(
                        (float) BitmapFactory.decodeFile(image.getAbsolutePath()).getWidth() / 3600,
                        (float) BitmapFactory.decodeFile(image.getAbsolutePath()).getHeight() / 2400);

        // resize the bitmap
        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(
                        BitmapFactory.decodeFile(image.getAbsolutePath()),
                        (int) (BitmapFactory.decodeFile(image.getAbsolutePath()).getWidth() / scaleFactor),
                        (int) (BitmapFactory.decodeFile(image.getAbsolutePath()).getHeight() / scaleFactor),
                        true);


        return resizedBitmap;
    }

    private void processFace(){
        mProgressDialog = new ProgressDialog(ProfileActivity.this);
        mProgressDialog.setMessage("Processing Image. Please wait ...");
        mProgressDialog.show();

        FaceDetector detector = new FaceDetector.Builder(ProfileActivity.this)
                .setProminentFaceOnly(true)
                .build();

        Bitmap source = getBitmap(new File(currentPhotoPath));
        Frame outputFrame = new Frame.Builder().setBitmap(source).build();
        detector.detect(outputFrame);
        SparseArray<Face> faces = detector.detect(outputFrame);

        mFaceBitmap = getFace(faces);

    }

    private Bitmap getFace(SparseArray<Face> faces) {
        mProgressDialog.setMessage("Detecting face...");
        // Task completed successfully
        if (faces.size() == 0) {
            mFaceBitmap = null;

            mProgressDialog.dismiss();
        } else {
            Bitmap source = mBitmap;

            for (int i = 0; i < faces.size(); ++i) {
                Face face = faces.get(i);

                if (face != null) {
                    Bitmap faceBitmap = Bitmap.createBitmap(source,
                            (int) face.getPosition().x,
                            (int) face.getPosition().y,
                            (int) face.getWidth(),
                            (int) face.getHeight());

                    saveRecognizedFace(faceBitmap);
                }else{
                    processFace();
                }
            }

        }
        return mFaceBitmap;
    }

    private void saveRecognizedFace(Bitmap faceBitmap) {
        final CallbackInterface callback = new CallbackInterface() {

            public void done(Exception e) {
                if (e == null) {
                    Log.d(TAG, "onImageSavedCallback: image saved!");
                } else {
                    Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                }
            }
        };


        String filename = profile.getId_number()+".jpg";

        ImageSaver imageSaver = new ImageSaver(
                faceBitmap,
                ProfileActivity.this.getExternalFilesDir(null),
                callback, filename
        );
        imageSaver.run();

        mFaceBitmap = faceBitmap;
        profile.setHasImage(true);
        localDatabase.databaseService().updateProfile(profile);
        mProgressDialog.dismiss();


        finish();
        startActivity(getIntent().putExtra("id",profile.getId()));


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        // optional depending on your needs super.onBackPressed();
    }
}