package com.louiskoyio.nationalidreader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
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
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.louiskoyio.nationalidreader.database.LocalDatabase;
import com.louiskoyio.nationalidreader.models.Profile;
import com.louiskoyio.nationalidreader.utilities.ImageSaver;
import com.louiskoyio.nationalidreader.utilities.NationalIDImageProcessor;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {

    private static final String TAG = "ResultsActivity";

    private ImageView mImageView,imgFace;
    private EditText txtName,txtIdNumber;
    private Button takePicture,saveDetails,viewProfiles;
    public Bitmap mBitmap,mFaceBitmap;
    public String predictedName;
    public String predictedIDNumber;
    static final int REQUEST_TAKE_PHOTO = 100;
    public List<String> processedResults;
    private String currentPhotoPath;
    private LocalDatabase localDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        localDatabase = LocalDatabase.getInstance(this);

        txtIdNumber = findViewById(R.id.txtIDno);
        txtName = findViewById(R.id.txtName);
        takePicture = findViewById(R.id.button_camera);
        saveDetails = findViewById(R.id.button_save);
        viewProfiles = findViewById(R.id.button_profiles);
        mImageView = findViewById(R.id.image_view);
        imgFace = findViewById(R.id.imgFace);


        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // startActivity(new Intent(MainActivity.this,CameraActivity.class));
                dispatchTakePictureIntent();

            }
        });
        viewProfiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ResultsActivity.this,ViewProfilesActivity.class));
            }
        });

        saveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfile();
            }
        });



        Intent intent = getIntent();
        if(intent!=null){
            init();
        }
    }



    public void init(){
        Intent intent = getIntent();

        File sourceImage;
        Boolean faceFound = intent.getExtras().getBoolean("face");
        currentPhotoPath = intent.getExtras().getString("currentPhotoPath");

        sourceImage = new File(currentPhotoPath);
        if(faceFound){
            mFaceBitmap = BitmapFactory.decodeFile(getExternalFilesDirs(null)+"/temp_face.jpg");
            imgFace.setImageBitmap(mFaceBitmap);
        }else
            showToast("No face found");

        txtName.setText(intent.getExtras().getString("name"));
        txtIdNumber.setText(intent.getExtras().getString("idNumber"));

        mBitmap = getBitmap(sourceImage);
        mImageView.setImageBitmap(mBitmap);

    }


    public Bitmap getBitmap(File image){
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
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void saveProfile() {

        final String name, idNumber;
        if(!txtName.getText().toString().isEmpty())
            name = txtName.getText().toString().trim().toUpperCase();
        else {
            name="";
            txtName.requestFocus();
            txtName.setError("Name is empty");
        }
        if(!txtIdNumber.getText().toString().isEmpty())
            idNumber = txtIdNumber.getText().toString().trim().toUpperCase();
        else {
            idNumber="";
            txtName.requestFocus();
            txtName.setError("ID number is empty is empty");
        }

        final Bitmap faceBitmap = ((BitmapDrawable) imgFace.getDrawable()).getBitmap();
        final int db_id = localDatabase.databaseService().getAllProfiles().size();

        AlertDialog.Builder saveProfileDialogBuilder = new AlertDialog.Builder(ResultsActivity.this);
        saveProfileDialogBuilder.setTitle("Confirm Details");
        saveProfileDialogBuilder.setMessage("Save details?\n\nName:\t\t"+name+"\n\nID Number:\t\t"+idNumber);
        saveProfileDialogBuilder.setCancelable(true);
        saveProfileDialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Profile newProfile = new Profile();
                newProfile.setName(name);
                newProfile.setId_number(idNumber);
                newProfile.setId(db_id);


                if(faceBitmap != null) {
                    final CallbackInterface callback = new CallbackInterface() {
                        @Override
                        public void done(Exception e) {
                            if (e == null) {
                                Log.d(TAG, "onImageSavedCallback: image saved!");
                                showToast("Face image saved");
                            } else {
                                Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                                showToast("Error saving face image");
                            }
                        }
                    };

                    ImageSaver imageSaver = new ImageSaver(
                            faceBitmap,
                            getApplicationContext().getExternalFilesDir(null),
                            callback, (idNumber + ".jpg"));

                    imageSaver.run();

                    localDatabase.databaseService().saveProfile(newProfile);
                    showToast("Profile saved successfully!");
                }else
                    showToast("Error saving details. No image found");
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



    private void savePicture() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

        Intent intent = new Intent();
        intent.putExtra("currentPhotoPath",currentPhotoPath);
        startActivity(intent);



    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "temp_id_image";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            savePicture();

        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }



}