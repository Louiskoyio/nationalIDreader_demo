package com.louiskoyio.nationalidreader;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.louiskoyio.nationalidreader.database.ApiService;
import com.louiskoyio.nationalidreader.database.LocalDatabase;
import com.louiskoyio.nationalidreader.database.RetrofitClient;
import com.louiskoyio.nationalidreader.models.Profile;
import com.louiskoyio.nationalidreader.models.ProfileApi;
import com.louiskoyio.nationalidreader.utilities.BitmapUtils;
import com.louiskoyio.nationalidreader.utilities.ImageSaver;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsActivity extends AppCompatActivity {

    private static final String TAG = "ResultsActivity";
    public String predictedName;

    private ImageView mImageView, imgFace;
    public String predictedIDNumber;
    private Button takePicture, saveDetails, viewProfiles;
    public Bitmap mBitmap, mFaceBitmap;
    private TextView allInfo, allValidInfo, removedInfo;
    private String currentPhotoPath;
    private EditText txtName, txtIdNumber, txtSex, txtDoB, txtDistrict, txtPoI, txtDoI;
    private ApiService apiService;
    private ProgressDialog mProgressDialog;
    private Boolean rotated = false;
    private Boolean faceFound = false;
    private byte[] mBitmapByteArray;
    private LocalDatabase localDatabase;
    private List<String> undesiredWords;

    public static String convertToProperDateFormat(String date) {

        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MM-yyyy");

        try {
            Date date1 = format.parse(date);
            date = displayFormat.format(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        localDatabase = LocalDatabase.getInstance(this);
        apiService = RetrofitClient.getClient(ResultsActivity.this).create(ApiService.class);


        txtIdNumber = findViewById(R.id.txtIDno);
        txtName = findViewById(R.id.txtName);
        txtSex = findViewById(R.id.txtSex);
        txtDoB = findViewById(R.id.txtDoB);
        txtPoI = findViewById(R.id.txtPoI);
        txtDoI = findViewById(R.id.txtDateOfIssue);
        txtDistrict = findViewById(R.id.txtDistrict);

        takePicture = findViewById(R.id.button_camera);
        saveDetails = findViewById(R.id.button_save);
        viewProfiles = findViewById(R.id.button_profiles);
        mImageView = findViewById(R.id.image_view);
        imgFace = findViewById(R.id.imgFace);
/*
        allInfo = findViewById(R.id.allInfo);
        allValidInfo = findViewById(R.id.allValidInfo);
        removedInfo = findViewById(R.id.removedInfo);

 */

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ResultsActivity.this, CameraActivity.class));

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
        if (intent != null) {
            init();
        }
    }

    private void saveProfile() {

        final String name, idNumber, dob, sex, district, poi, doi;

        if (!txtName.getText().toString().isEmpty())
            name = txtName.getText().toString().trim().toUpperCase();
        else {
            name = "";
        }
        if (!txtIdNumber.getText().toString().isEmpty())
            idNumber = txtIdNumber.getText().toString().trim().toUpperCase();
        else {
            idNumber = "";
        }
        if (!txtDoB.getText().toString().isEmpty())
            dob = txtDoB.getText().toString().trim().toUpperCase();
        else {
            dob = "";
        }
        if (!txtSex.getText().toString().isEmpty())
            sex = txtSex.getText().toString().trim().toUpperCase();
        else {
            sex = "";
        }
        if (!txtDistrict.getText().toString().isEmpty())
            district = txtDistrict.getText().toString().trim().toUpperCase();
        else {
            district = "";
        }
        if (!txtPoI.getText().toString().isEmpty())
            poi = txtPoI.getText().toString().trim().toUpperCase();
        else {
            poi = "";
        }
        if (!txtDoI.getText().toString().isEmpty())
            doi = txtDoI.getText().toString().trim().toUpperCase();
        else {
            doi = "";
        }


        if (name.isEmpty()) {
            txtName.requestFocus();
            txtName.setError("Name is empty");
        } else if (idNumber.isEmpty()) {
            txtIdNumber.requestFocus();
            txtIdNumber.setError("ID number is empty");
        } else if (dob.isEmpty()) {
            txtDoB.requestFocus();
            txtDoB.setError("DOB is empty");
        } else if (sex.isEmpty()) {
            txtSex.requestFocus();
            txtSex.setError("Field is empty");
        } else if (district.isEmpty()) {
            txtDistrict.requestFocus();
            txtDistrict.setError("District is empty");
        } else if (poi.isEmpty()) {
            txtPoI.requestFocus();
            txtPoI.setError("Place of issue is empty");
        } else if (doi.isEmpty()) {
            txtDoI.requestFocus();
            txtDoI.setError("Date of issue is empty");
        } else {
            final int db_id = localDatabase.databaseService().getAllProfiles().size();

            AlertDialog.Builder saveProfileDialogBuilder = new AlertDialog.Builder(ResultsActivity.this);
            saveProfileDialogBuilder.setTitle("Confirm Details");
            saveProfileDialogBuilder.setMessage("Save details?\nName:\t" + name + "\nID Number:\t" + idNumber);
            saveProfileDialogBuilder.setCancelable(true);
            saveProfileDialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Profile newProfile = new Profile();
                    newProfile.setName(name);
                    newProfile.setId_number(idNumber);
                    newProfile.setId(db_id);
                    newProfile.setDob(dob);
                    newProfile.setSex(sex);
                    newProfile.setSynced(false);
                    newProfile.setDistrict_of_birth(district);
                    newProfile.setPlace_of_issue(poi);
                    newProfile.setDoi(doi);

                    ProfileApi apiFormatProfile = new ProfileApi();

                    apiFormatProfile.setId_number(idNumber);
                    apiFormatProfile.setName(name);
                    apiFormatProfile.setDate_of_birth(dob);
                    apiFormatProfile.setGender(sex);
                    apiFormatProfile.setDistrict_of_birth(district);
                    apiFormatProfile.setPlace_of_issue(poi);
                    apiFormatProfile.setDate_of_issue(doi);


                    Bitmap faceBitmap = null;
                    if (imgFace.getDrawable() != null) {
                        faceBitmap = ((BitmapDrawable) imgFace.getDrawable()).getBitmap();
                        // apiFormatProfile.setImg(BitmapUtils.convertBitmapToNv21Bytes(faceBitmap));
                    }


                    if (faceBitmap != null) {
                        final CallbackInterface callback = new CallbackInterface() {
                            @Override
                            public void done(Exception e) {
                                if (e == null) {
                                    Log.d(TAG, "onImageSavedCallback: image saved!");
                                    newProfile.setHasImage(true);
                                    showToast("Face image saved");
                                } else {
                                    Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                                    newProfile.setHasImage(false);
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
                    } else {
                        newProfile.setHasImage(false);
                        //  showToast("Error saving details. No image found");
                        localDatabase.databaseService().saveProfile(newProfile);

                        List<Profile> profiles = localDatabase.databaseService().getAllProfiles();
                        long id = profiles.get(((localDatabase.databaseService().getAllProfiles().size()) - 1)).getId();

                        startActivity(new Intent(ResultsActivity.this, ProfileActivity.class).putExtra("camera_frame", id));
                    }


                    apiService.addProfile(apiFormatProfile).enqueue(new Callback<ProfileApi>() {
                        @Override
                        public void onResponse(Call<ProfileApi> call, Response<ProfileApi> response) {
                            if (response.isSuccessful()) {
                                newProfile.setSynced(true);
                                localDatabase.databaseService().updateProfile(newProfile);
                            }
                        }

                        @Override
                        public void onFailure(Call<ProfileApi> call, Throwable t) {

                        }
                    });

                    // send to api
                    //new DataSender(ResultsActivity.this,newProfile,faceBitmap).sendProfileToApi();

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
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void init() {
        Intent intent = getIntent();

        File sourceImage;
        currentPhotoPath = getExternalFilesDir(null) + "/temp_image.jpg";
        //getExternalFilesDirs(null)+"/temp_image.jpg"
        sourceImage = new File(getExternalFilesDir(null) + "/temp_image.jpg");

        mBitmap = getBitmap(sourceImage);
        mImageView.setImageBitmap(mBitmap);
        mProgressDialog = new ProgressDialog(ResultsActivity.this);
        mProgressDialog.setMessage("Processing Image. Please wait ...");
        mProgressDialog.show();
        processImage();
        prepareResources();
    }

    private void prepareResources() {
        undesiredWords = new ArrayList<>();

        undesiredWords.add("JAMHURI");
        undesiredWords.add("YA");
        undesiredWords.add("KENYA");
        undesiredWords.add("REPUBLIC");
        undesiredWords.add("OF");
        undesiredWords.add("SERIAL");
        undesiredWords.add("NUMBER");
        undesiredWords.add("MALE");
        undesiredWords.add("FEMALE");
        undesiredWords.add("FULL");
        undesiredWords.add("NAMES");
        undesiredWords.add("DATE");
        undesiredWords.add("BIRTH");
        undesiredWords.add("SEX");
        undesiredWords.add("DISTRICT");
        undesiredWords.add("PLACE");
        undesiredWords.add("ISSUE");
        undesiredWords.add("HOLDER'S");
        undesiredWords.add("SIGN");


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

    private void processImage() {


        mProgressDialog.setMessage("Processing Image. Please wait ...");


        final InputImage image = InputImage.fromBitmap(mBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {

                            public void onSuccess(Text texts) {

                                List<String> words = new ArrayList<>();
                                List<Text.TextBlock> blocks = texts.getTextBlocks();
                                for (int i = 0; i < blocks.size(); i++) {
                                    List<Text.Line> lines = blocks.get(i).getLines();
                                    for (int j = 0; j < lines.size(); j++) {
                                        for (int k = 0; k < lines.size(); k++) {
                                            words.add(lines.get(k).getText());
                                        }
                                    }
                                }
                                if (!imageIsCorrect(blocks)) {

                                    correctImage();
                                    processImage();
                                } else {
                                    if (rotated) {
                                        final CallbackInterface callback = new CallbackInterface() {

                                            public void done(Exception e) {
                                                if (e == null) {
                                                    Log.d(TAG, "onImageSavedCallback: image saved!");
                                                } else {
                                                    Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                                                }
                                            }
                                        };

                                        ImageSaver imageSaver = new ImageSaver(
                                                mBitmap,
                                                ResultsActivity.this.getExternalFilesDir(null),
                                                callback, "rotated_image.jpg"
                                        );
                                        imageSaver.run();

                                    }
                                    mImageView.setImageBitmap(mBitmap);
                                    mProgressDialog.setMessage("Processing text.");
                                    getData(blocks);
                                }

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {

                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });


    }

    public void getData(List<Text.TextBlock> blocks) {

        mProgressDialog.setMessage("Processing text..");

        List<String> allLines = new ArrayList<>();
        List<String> allInvalidLines = new ArrayList<>();
        List<String> allNumbers = new ArrayList<>();
        List<String> allWords = new ArrayList<>();
        List<String> allDates = new ArrayList<>();

        String allLinesString = "";
        String allValidLinesString = "";
        String allInvalidLinesString = "";

        if (blocks.size() == 0) {
            predictedName = "";
            predictedIDNumber = "";
        }

        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {

                allLines.add(lines.get(j).getText());

                // loop thro' all words
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                    // get current word
                    String word = elements.get(k).getText();
                    allWords.add(word);


                    if (StringUtils.isNumeric(word) && !containsDot(word) && word.length() == 8) {
                        allNumbers.add(word);
                    }


                    // if the number has a dot i.e is a date
                    if (containsDot(word) && containsNumber(word)) {

                        allDates.add(lines.get(j).getText());
                    }
                    //give the line a temp camera_frame

                    // check if word is valid
                /*    if (isWordValid(word) && !invalidLines.contains(lineId)) {
                        String lineToBeAdded = lines.get(j).getText();
                        if (!allValidLines.contains(lineToBeAdded))
                            allValidLines.add(lineToBeAdded);
                    } else if (isWordValid(word) && invalidLines.contains(lineId)) {
                        allValidLines.add(word);
                    }
                    if (!isWordValid(word) && !invalidLines.contains(lineId)) {
                        invalidLines.add(i + "-" + j);
                        allInvalidLines.add(lines.get(j).getText());
                    }*/

                }
            }
        }


        for (String line : allLines) {
            allLinesString = allLinesString + line + "\n";
        }

        List<String> linesWithNumbers = new ArrayList<>();

        for (String line : allInvalidLines) {
            allInvalidLinesString = allInvalidLinesString + line + "\n";
        }


        mProgressDialog.setMessage("Processing text...");

        getName(allLines);
        getIdDNumber(allNumbers);
        getGender(allWords);
        getPoIAndDistrict(allLines);
        getDoBAndDoI(allDates);

/*
        allInfo.setText("ALL:\n" + allLinesString);
        allValidInfo.setText("VALID:\n" + allDates);
        removedInfo.setText("ALL NUMBERS:\n" + allNumbers);
*/

        processFace();

    }

    private void processFace() {
        mProgressDialog.setMessage("Processing face...");

        FaceDetector detector;
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();


        mBitmapByteArray = BitmapUtils.convertBitmapToNv21Bytes(mBitmap);

        InputImage image1 = InputImage.fromByteArray(
                mBitmapByteArray,
                /* image width */mBitmap.getWidth(),
                /* image height */mBitmap.getHeight(),
                0,
                InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
        );


        detector = FaceDetection.getClient(options);


        Task<List<com.google.mlkit.vision.face.Face>> result = detector.process(image1)
                .addOnSuccessListener(
                        new OnSuccessListener<List<com.google.mlkit.vision.face.Face>>() {
                            @Override
                            public void onSuccess(List<com.google.mlkit.vision.face.Face> faces) {
                                mFaceBitmap = getFace(faces);
                                detector.close();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                detector.close();
                            }
                        });

    }

    private Bitmap getFace(List<Face> faces) {
        mProgressDialog.setMessage("Detecting face...");
        // Task completed successfully
        if (faces.size() == 0) {
            mFaceBitmap = null;
            faceFound = false;
            mProgressDialog.dismiss();

            showToast("No faces detected");
        } else {


            for (int i = 0; i < faces.size(); ++i) {
                com.google.mlkit.vision.face.Face face = faces.get(i);
                Rect rect = face.getBoundingBox();


                if (face != null) {
                    File file = new File(currentPhotoPath);
                    Bitmap bitmap = getBitmap(file);
                    Bitmap faceCrop = Bitmap.createBitmap(
                            bitmap,
                            rect.left,
                            rect.top,
                            rect.width(),
                            rect.height());

                    imgFace.setImageBitmap(faceCrop);
                    mProgressDialog.dismiss();
                    //  saveRecognizedFace(faceCrop);
                } else {
                    processFace();
                }
            }

        }
        return mFaceBitmap;
    }

    public String cleanupNumber(String text) {
        char[] textArray = text.toCharArray();

        List<Character> removedLetters = new ArrayList<>();
        for (Character character : textArray) {
            if (Character.isDigit(character))
                removedLetters.add(character);

        }

        StringBuilder sb = new StringBuilder();

        for (Character ch : removedLetters) {
            sb.append(ch);
        }


        String cleanedNumber = sb.toString();

        return cleanedNumber;
    }

    public Boolean containsDot(String line) {
        Boolean hasDot = false;

        char[] splitLine = line.toCharArray();
        for (Character character : splitLine) {
            if (character == '.')
                hasDot = true;
        }

        return hasDot;
    }

    public Boolean containsNumber(String line) {
        Boolean hasNumber = false;

        char[] splitLine = line.toCharArray();
        for (Character character : splitLine) {
            if (Character.isDigit(character))
                hasNumber = true;
        }

        return hasNumber;
    }

    private void getGender(List<String> allWords) {
        String gender = "";
        MyString maleCheck = new MyString("MALE");
        MyString femaleCheck = new MyString("FEMALE");

        //check if male

        for (String word : allWords) {
            if (maleCheck.isSimilar(word)) {
                gender = "MALE";
            } else if (femaleCheck.isSimilar(word)) {
                gender = "FEMALE";
            }
        }


        txtSex.setText(gender);
    }

    private void getDoBAndDoI(List<String> allDates) {
        List<String> cleanedDates = new ArrayList<>();
        List<String> formattedDates = new ArrayList<>();


        for (String date : allDates) {
            cleanedDates.add(cleanupNumber(date));
        }

        for (String cleanedDate : cleanedDates) {
            formattedDates.add(convertToProperDateFormat(cleanedDate));
        }

        if (formattedDates.size() > 0) {
            String dob = formattedDates.get(0);
            txtDoB.setText(dob);
        }
        if (formattedDates.size() > 1) {
            String doi = formattedDates.get(formattedDates.size() - 1);
            txtDoI.setText(doi);
        }
    }

    private void getName(List<String> allLines) {
        String name = null;

        MyString nameCheck = new MyString("FULL NAMES");

        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);

            if (nameCheck.isSimilar(line)) {
                name = allLines.get((i + 1));
            }

            if (name != null)
                txtName.setText(name);

        }
    }

    private void getIdDNumber(List<String> allNumbers) {


        if (allNumbers.size() > 0)
            txtIdNumber.setText(allNumbers.get(0));


    }

    private void getPoIAndDistrict(List<String> allLines) {
        String district = null, poi = null;

        MyString districtCheck = new MyString("DISTRICT OF BIRTH");
        MyString poiCheck = new MyString("PLACE OF ISSUE");

        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            if (districtCheck.isSimilar(line)) {
                district = allLines.get((i + 1));
            } else if (poiCheck.isSimilar(line) && poi == null) {
                poi = allLines.get((i + 1));
            }
        }

        if (district != null)
            txtDistrict.setText(district);
        if (poi != null)
            txtPoI.setText(poi);
    }

    public Boolean hasValidWords(List<String> words) {
        Boolean isAValidImage;
        int i = 0;

        MyString comparator = new MyString("JAMHURI");
        for (String word : words) {
            System.out.println("==================================================" + word);
            if (comparator.isSimilar(word)) {
                i++;
            }
        }

        isAValidImage = i > 0;

        return isAValidImage;

    }


    public Bitmap correctedImage(Bitmap bitmap, float angle) {
        mProgressDialog.setMessage("Rotating image...");
        final Matrix matrix = new Matrix();
        Bitmap bmRotated;

        matrix.setRotate(angle);
        bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        rotated = true;

        return bmRotated;
    }

    private Boolean imageIsCorrect(List<Text.TextBlock> text) {
        List<String> fetchedWords = new ArrayList<>();

        for (int i = 0; i < text.size(); i++) {
            List<Text.Line> lines = text.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    fetchedWords.add(elements.get(k).getText());
                }
            }
        }


        return fetchedWords.size() > 8 && hasValidWords(fetchedWords);
    }


    private void correctImage() {
        mProgressDialog.setMessage("Correcting image...");
        if (!rotated) {
            mBitmap = correctedImage(mBitmap, 90);
            rotated = true;
        } else {
            Bitmap originalBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            mBitmap = correctedImage(originalBitmap, -90);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ResultsActivity.this, CameraActivity.class));
    }
}