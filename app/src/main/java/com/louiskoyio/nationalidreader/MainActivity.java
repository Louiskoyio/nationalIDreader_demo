package com.louiskoyio.nationalidreader;


import android.app.ProgressDialog;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import org.apache.commons.lang3.StringUtils;

import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ImageView mImageView, imgFace;
    private EditText txtName, txtIdNumber;
    private Button takePicture, saveDetails, viewProfiles;
    private TextView allInfo, allValidInfo, removedInfo;
    private Button mNewId, mViewProfiles;
    private ProgressDialog mProgressDialog;
    static final int REQUEST_TAKE_PHOTO = 100;
    private String currentPhotoPath;
    private Boolean faceFound = false;
    public String predictedName;
    public String predictedIDNumber;
    private LinearLayout  resultLayout;
    private ConstraintLayout normalLayout;
    private Bitmap mBitmap, mFaceBitmap;
    private Boolean newCapture = false;
    private Boolean rotated = false;
    private LocalDatabase localDatabase;


    /**
     * Number of results to show in the UI.
     */
    private static final int RESULTS_TO_SHOW = 3;

    /**
     * Dimensions of inputs.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                //startActivity(new Intent(MainActivity.this, CameraActivity.class));
                dispatchTakePictureIntent();

            }
        });
        viewProfiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ViewProfilesActivity.class));
            }
        });

        saveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfile();
            }
        });


        normalLayout = findViewById(R.id.normalLinearLayout);
        resultLayout = findViewById(R.id.resultsLinearLayout);
        allInfo = findViewById(R.id.allInfo);
        allValidInfo = findViewById(R.id.allValidInfo);
        removedInfo = findViewById(R.id.removedInfo);
        mViewProfiles = findViewById(R.id.button_view);
        mNewId = findViewById(R.id.button_new);



        mNewId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startActivity(new Intent(MainActivity.this, CameraActivity.class));

                dispatchTakePictureIntent();

            }
        });

        mViewProfiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ViewProfilesActivity.class));
            }
        });


        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {

         /*   newCapture = intent.getExtras().getBoolean("newCapture");

            if (newCapture) {


                processImage();
            }
*/
        }

    }

    private void saveProfile() {

        final String name, idNumber;
        if (!txtName.getText().toString().isEmpty())
            name = txtName.getText().toString().trim().toUpperCase();
        else {
            name = "";
            txtName.requestFocus();
            txtName.setError("Name is empty");
        }
        if (!txtIdNumber.getText().toString().isEmpty())
            idNumber = txtIdNumber.getText().toString().trim().toUpperCase();
        else {
            idNumber = "";
            txtName.requestFocus();
            txtName.setError("ID number is empty is empty");
        }

        final Bitmap faceBitmap = ((BitmapDrawable) imgFace.getDrawable()).getBitmap();
        final int db_id = localDatabase.databaseService().getAllProfiles().size();

        AlertDialog.Builder saveProfileDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        saveProfileDialogBuilder.setTitle("Confirm Details");
        saveProfileDialogBuilder.setMessage("Save details?\n\nName:\t\t" + name + "\n\nID Number:\t\t" + idNumber);
        saveProfileDialogBuilder.setCancelable(true);
        saveProfileDialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Profile newProfile = new Profile();
                newProfile.setName(name);
                newProfile.setId_number(idNumber);
                newProfile.setId(db_id);


                if (faceBitmap != null) {
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
                } else
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

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public Bitmap correctedImage(Bitmap bitmap) {
        mProgressDialog.setMessage("Rotating image...");
        Matrix matrix = new Matrix();
        Bitmap bmRotated = bitmap;


        matrix.setRotate(90);
        bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return bmRotated;
    }


    private void processImage() {

        mProgressDialog.setMessage("Processing Image. Please wait ...");


        resultLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);

        if(!rotated) {
            File imageFile = new File(currentPhotoPath);
            mBitmap = getBitmap(imageFile);
        }
        final InputImage image = InputImage.fromBitmap(mBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {

                            public void onSuccess(Text texts) {

                                List<String> words = new ArrayList<>();
                                List<Text.TextBlock> blocks = texts.getTextBlocks();
                                for(int i=0;i<blocks.size();i++){
                                    List<Text.Line> lines = blocks.get(i).getLines();
                                    for(int j=0;j<lines.size();j++){
                                        for(int k=0;k<lines.size();k++){
                                            words.add(lines.get(k).getText());
                                        }
                                    }
                                }
                                if(words.size()<8){
                                    mProgressDialog.setMessage("Fixing image...");
                                    mBitmap = correctedImage(mBitmap);
                                    rotated = true;
                                    processImage();
                                }else{
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
                                                MainActivity.this.getExternalFilesDir(null),
                                                callback, "rotated_image.jpg"
                                        );
                                        imageSaver.run();

                                    }

                                    mProgressDialog.setMessage("Processing text.");
                                        getNameAndIDNumber(texts);
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

    public void getNameAndIDNumber(Text texts) {

        mProgressDialog.setMessage("Processing text..");

        List<String> allLines = new ArrayList<>();
        List<String> allValidLines = new ArrayList<>();
        List<String> allInvalidLines = new ArrayList<>();
        List<String> predictedResults = new ArrayList<>();
        List<String> allNumbers = new ArrayList<>();
        List<String> validNumbers = new ArrayList<>();

        String allLinesString = "";
        String allValidLinesString = "";
        String allInvalidLinesString = "";

        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            predictedName = "";
            predictedIDNumber = "";
        }


        List<String> invalidLines = new ArrayList<>();

        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {

                allLines.add(lines.get(j).getText());

                // loop thro' all words
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                    // get current word
                    String word = elements.get(k).getText();

                    // if the number has a dot i.e is a date
                    if (containsDot(word))
                        allNumbers.add(word);

                    //give the line a temp id
                    String lineId = i + "-" + j;

                    // check if word is valid
                    if (isWordValid(word) && !invalidLines.contains(lineId)) {
                        String lineToBeAdded = lines.get(j).getText();
                        if (!allValidLines.contains(lineToBeAdded))
                            allValidLines.add(lineToBeAdded);
                    } else if (isWordValid(word) && invalidLines.contains(lineId)) {
                        allValidLines.add(word);
                    }
                    if (!isWordValid(word) && !invalidLines.contains(lineId)) {
                        invalidLines.add(i + "-" + j);
                        allInvalidLines.add(lines.get(j).getText());
                    }

                }
            }
        }


        for (String line : allLines) {
            allLinesString = allLinesString + line + "\n";
        }

        int linesWithNumbers = 0;
        int linesWithWords = 0;
        for (String line : allValidLines) {


            if (containsNumber(line)) {
                String newLine = cleanupNumber(line);

                if (newLine.length() <= 8 && linesWithNumbers < 1 && !containsDot(line)) {
                    allValidLinesString = allValidLinesString + newLine + "\n";
                    predictedResults.add(newLine);
                    linesWithNumbers++;
                }
            } else {
                if (linesWithWords < 1 && !allInvalidLines.contains(line) && lineWithMoreThanTwoWords(line)) {
                    allValidLinesString = allValidLinesString + line + "\n";
                    predictedResults.add(line);

                    linesWithWords++;
                }
            }
        }
        for (String line : allInvalidLines) {
            allInvalidLinesString = allInvalidLinesString + line + "\n";
        }


        if (allLines.size() > 0) {
            int indexOfName = 0,indexOfNumber =0;

            for(String prediction: predictedResults){
                if(!StringUtils.isNumeric(prediction))
                    indexOfName = predictedResults.indexOf(prediction);
                else
                    indexOfNumber = predictedResults.indexOf(prediction);

            }

            if (allNumbers.size() > 0) {
                validNumbers = validNumbers(allNumbers);

                if (validNumbers.size() > 0) {
                    predictedIDNumber = validNumbers.get(0);
                } else {
                    predictedIDNumber = predictedResults.get(indexOfNumber);
                    if (!StringUtils.isNumeric(predictedIDNumber))
                        predictedIDNumber = "";
                }
            }

            if (predictedResults.size() > 0) {
                predictedName = predictedResults.get(indexOfName);
            } else {
                predictedName = "";
            }
            mProgressDialog.setMessage("Processing text...");

            FaceDetector detector = new FaceDetector.Builder(MainActivity.this)
                    .setProminentFaceOnly(true)
                    .build();
            Bitmap source = mBitmap;
            Frame outputFrame = new Frame.Builder().setBitmap(source).build();
            detector.detect(outputFrame);
            SparseArray<Face> faces = detector.detect(outputFrame);

            mProgressDialog.setMessage("Processing face...");
            mFaceBitmap = getFace(faces);


            allInfo.setText("ALL:\n" + allLinesString);
            allValidInfo.setText("VALID:\n" + allValidLinesString);
            removedInfo.setText("REMOVED:\n" + allInvalidLines);
        }


    }

    private Bitmap getFace(SparseArray<Face> faces) {
        mProgressDialog.setMessage("Detecting face...");
        // Task completed successfully
        if (faces.size() == 0) {
            mFaceBitmap = null;
            faceFound = false;
            mProgressDialog.dismiss();

            displayResults();
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

        ImageSaver imageSaver = new ImageSaver(
                faceBitmap,
                MainActivity.this.getExternalFilesDir(null),
                callback, "temp_face.jpg"
        );
        imageSaver.run();

        mFaceBitmap = faceBitmap;
        faceFound = true;

        displayResults();


    }

    public void displayResults() {
        mProgressDialog.dismiss();
        mImageView.setImageBitmap(mBitmap);

        if (mFaceBitmap != null)
            imgFace.setImageBitmap(mFaceBitmap);

        txtName.setText(predictedName);
        txtIdNumber.setText(predictedIDNumber);


    }

    public Boolean isWordValid(String word) {
        Boolean valid = null;

        List<String> undesiredWords = new ArrayList<>();
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


        Boolean similarToUnwantedWord;

        int similarWords = 0;
        for (int i = 0; i < undesiredWords.size() - 1; i++) {
            MyString comparator = new MyString(word);
            similarToUnwantedWord = comparator.isSimilar(undesiredWords.get(i));

            if (similarToUnwantedWord)
                similarWords++;
        }


        if (similarWords > 0)
            valid = false;
        else if (similarWords == 0)
            valid = true;


        return valid;
    }

    public Boolean lineWithMoreThanTwoWords(String line) {
        Boolean hasMoreThanOneWord = false;
        String[] splitLine = line.split(" ");

        if (splitLine.length > 2)
            hasMoreThanOneWord = true;

        return hasMoreThanOneWord;
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

    public List<String> validNumbers(List<String> allNumbers) {
        List<String> validNumbers = new ArrayList<>();
        List<String> filteredByContent = new ArrayList<>();
        List<String> cleanedValidNumbers = new ArrayList<>();


        //filter numbers by content - to remove dates
        for (String number : allNumbers) {
            if (!containsDot(number))
                filteredByContent.add(number);
        }


        //clean all numbers - remove letters
        for (String number : filteredByContent) {
            String cleanedNumber = cleanupNumber(number);
            cleanedValidNumbers.add(cleanedNumber);
        }


        //filter numbers by length - to remove serial number
        for (String number : cleanedValidNumbers) {
            if (number.length() == 8)
                validNumbers.add(number);
        }

        return validNumbers;
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Processing Image. Please wait ...");
        mProgressDialog.show();
        processImage();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
        }
    }
}
