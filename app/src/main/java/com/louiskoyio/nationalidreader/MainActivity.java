package com.louiskoyio.nationalidreader;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import org.apache.commons.lang3.StringUtils;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;

import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.louiskoyio.nationalidreader.database.LocalDatabase;
import com.louiskoyio.nationalidreader.models.Profile;
import com.louiskoyio.nationalidreader.utilities.BitmapUtils;
import com.louiskoyio.nationalidreader.utilities.ImageSaver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ImageView mImageView, imgFace;
    private EditText txtName, txtIdNumber,txtSex,txtDoB,txtDistrict,txtPoI,txtDoI;
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
    private byte[] mBitmapByteArray;
    private Boolean rotated = false;
    private int rotations = 0;
    private LocalDatabase localDatabase;
    private List<String> undesiredWords;
    private List<String> words;
    private Text grabbedText;


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
        txtSex = findViewById(R.id.txtSex);
        txtDoB = findViewById(R.id.txtDoB);
        txtPoI =  findViewById(R.id.txtPoI);
        txtDoI= findViewById(R.id.txtDateOfIssue);
        txtDistrict = findViewById(R.id.txtDistrict);

        takePicture = findViewById(R.id.button_camera);
        saveDetails = findViewById(R.id.button_save);
        viewProfiles = findViewById(R.id.button_profiles);
        mImageView = findViewById(R.id.image_view);
        imgFace = findViewById(R.id.imgFace);

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startActivity(new Intent(MainActivity.this, CameraActivity.class));
                mBitmap = null;
                mImageView.setImageDrawable(null);
                imgFace.setImageDrawable(null);
                txtName.setText("");
                txtIdNumber.setText("");
                rotated = false;

                try {
                    deleteFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

        prepareResources();



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

    private void prepareResources(){
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


        words = new ArrayList<>();

    }

    private void saveProfile() {

        final String name, idNumber,dob,sex,district,poi,doi;

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
            txtName.setError("ID number is empty");
        }
        if (!txtDoB.getText().toString().isEmpty())
            dob = txtDoB.getText().toString().trim().toUpperCase();
        else {
            dob = "";
            txtDoB.requestFocus();
            txtDoB.setError("DOB is empty");
        }
        if (!txtSex.getText().toString().isEmpty())
            sex = txtSex.getText().toString().trim().toUpperCase();
        else {
            sex = "";
            txtSex.requestFocus();
            txtSex.setError("Gender is empty");
        }
        if (!txtDistrict.getText().toString().isEmpty())
            district = txtDistrict.getText().toString().trim().toUpperCase();
        else {
            district = "";
            txtDistrict.requestFocus();
            txtDistrict.setError("District is empty");
        }
        if (!txtPoI.getText().toString().isEmpty())
            poi = txtPoI.getText().toString().trim().toUpperCase();
        else {
            poi = "";
            txtPoI.requestFocus();
            txtPoI.setError("Place of issue is empty");
        }
        if (!txtDoI.getText().toString().isEmpty())
            doi = txtDoI.getText().toString().trim().toUpperCase();
        else {
            doi = "";
            txtDoI.requestFocus();
            txtDoI.setError("Date of issue is empty");
        }


        final int db_id = localDatabase.databaseService().getAllProfiles().size();

        AlertDialog.Builder saveProfileDialogBuilder = new AlertDialog.Builder(MainActivity.this);
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
                newProfile.setDistrict_of_birth(district);
                newProfile.setPlace_of_issue(poi);
                newProfile.setDoi(doi);

                Bitmap faceBitmap=null;
                if(imgFace.getDrawable()!=null) {
                    faceBitmap = ((BitmapDrawable) imgFace.getDrawable()).getBitmap();
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

                    List<Profile> profiles= localDatabase.databaseService().getAllProfiles();
                    int id = profiles.get(((localDatabase.databaseService().getAllProfiles().size())-1)).getId();

                    startActivity(new Intent(MainActivity.this,ProfileActivity.class).putExtra("id",id));
                }
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


    public Boolean hasValidWords(List<String> words){
        Boolean isAValidImage;
        int i=0;

        MyString comparator = new MyString("JAMHURI");
        for(String word:words){
            System.out.println("==================================================" + word);
            if(comparator.isSimilar(word)) {
                i++;
            }
        }

        if(i>0)
            isAValidImage = true;
        else
            isAValidImage = false;

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

    private Boolean imageIsCorrect(List<Text.TextBlock> text){
        List<String> fetchedWords = new ArrayList<>();

        for(int i = 0; i< text.size(); i++){
            List<Text.Line> lines = text.get(i).getLines();
            for(int j=0;j<lines.size();j++){
                List<Text.Element> elements = lines.get(j).getElements();
                for(int k=0;k<elements.size();k++){
                    fetchedWords.add(elements.get(k).getText());
                }
            }
        }


        if(fetchedWords.size()>8 && hasValidWords(fetchedWords)){
                return true;
        }else{
                return false;
        }
    }


    private void correctImage(){
        mProgressDialog.setMessage("Correcting image...");
        if(!rotated) {
            mBitmap = correctedImage(mBitmap,90);
            rotated = true;
        }else{
            Bitmap originalBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            mBitmap = correctedImage(originalBitmap,-90);
        }
    }

    private void processImage() {


        mProgressDialog.setMessage("Processing Image. Please wait ...");



        resultLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);

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
                                if(!imageIsCorrect(blocks)){

                                    correctImage();
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
                                    mImageView.setImageBitmap(mBitmap);
                                    mProgressDialog.setMessage("Processing text.");
                                        getNameAndIDNumber(blocks);
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

    public void getNameAndIDNumber(List<Text.TextBlock> blocks) {

        mProgressDialog.setMessage("Processing text..");

        List<String> allLines = new ArrayList<>();
        List<String> allValidLines = new ArrayList<>();
        List<String> allInvalidLines = new ArrayList<>();
        List<String> predictedResults = new ArrayList<>();
        List<String> allNumbers = new ArrayList<>();
        List<String> validNumbers = new ArrayList<>();
        List<String> allWords = new ArrayList<>();
        List<String> allDates = new ArrayList<>();

        String allLinesString = "";
        String allValidLinesString = "";
        String allInvalidLinesString = "";

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
                    allWords.add(word);



                    // if the number has a dot i.e is a date
                    if (containsDot(word)&&containsNumber(word)) {
                        allNumbers.add(word);
                        allDates.add(lines.get(j).getText());
                    }
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
            txtName.setText(predictedName);
            txtIdNumber.setText(predictedIDNumber);
            txtSex.setText(getGender(allWords));
            txtDistrict.setText(getPoIAndDistrict(allLines).get(0));
            txtPoI.setText(getPoIAndDistrict(allLines).get(1));


            List<String> dobDoI=getDoBAndDoI(allDates);
            String dob = dobDoI.get(0);
            if(dob!=null)
                txtDoB.setText(dob);

            String doi = dobDoI.get((dobDoI.size()-1));
            if(doi!=null)
                txtDoI.setText(doi);

            allInfo.setText("ALL:\n" + allLinesString);
            allValidInfo.setText("VALID:\n" + allDates);
            removedInfo.setText("REMOVED:\n" + allInvalidLines);

            processFace();

//            mProgressDialog.dismiss();

        }
    }



    private void tryFace(){
        mBitmap = mImageView.getDrawingCache();

    }


    private void processFace(){
        mProgressDialog.setMessage("Processing face...");

        FaceDetector detector;
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .enableTracking()
                .build();

        InputImage image = InputImage.fromBitmap(mBitmap, 0);

        mBitmapByteArray = BitmapUtils.convertBitmapToNv21Bytes(mBitmap);

        detector = FaceDetection.getClient(options);

       /* InputImage image = InputImage.fromByteArray(
                mBitmapByteArray,
               480,
                360,
                0,
                InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
        );*/





       // Frame outputFrame = new Frame.Builder().setBitmap(mBitmap).build();
       // SparseArray<Face> faces = detector.detect(outputFrame);

        Task<List<Face>> result = detector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
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
            displayResults();

            showToast("No faces detected");
        } else {


            for (int i = 0; i < faces.size(); ++i) {
                com.google.mlkit.vision.face.Face face = faces.get(i);
                Rect rect = face.getBoundingBox();



                if (face != null) {
/*                    Bitmap faceBitmap = Bitmap.createBitmap(source,
                            (int) face.getPosition().x,
                            (int) face.getPosition().y,
                            (int) face.getWidth(),
                            (int) face.getHeight());*/
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
                    saveRecognizedFace(faceCrop);
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
        mBitmap=getBitmap(f);
        mImageView.setImageBitmap(mBitmap);



        resultLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);
        processImage();
        //processFace();

    }

    private String getGender(List<String> allWords){
        String gender="";
        MyString maleCheck = new MyString("MALE");
        MyString femaleCheck = new MyString("FEMALE");

        //check if male

        for(String word: allWords){
            if(maleCheck.isSimilar(word)){
                gender = "MALE";
                return gender;
            }else if(femaleCheck.isSimilar(word)){
                gender = "FEMALE";
                return gender;
            }
        }

        return gender;
    }

    private List<String> getDoBAndDoI(List<String> allDates){
        List<String> dates = new ArrayList<>();
        List<String> cleanedDates = new ArrayList<>();
        List<String> formattedDates = new ArrayList<>();


        for( String date : allDates ){
            cleanedDates.add(cleanupNumber(date));
        }

        for( String cleanedDate : cleanedDates){
            formattedDates.add(convertToProperDateFormat(cleanedDate));
        }



        return formattedDates;
    }

    private List<String> getPoIAndDistrict(List<String> allLines){
        List<String> results = new ArrayList<>();
        String district = null,poi=null;

        MyString districtCheck = new MyString("DISTRICT OF BIRTH");
        MyString poiCheck = new MyString("PLACE OF ISSUE");

        for(int i =0;i<allLines.size();i++){
            String line = allLines.get(i);
            if(districtCheck.isSimilar(line)) {
                district = allLines.get((i + 1));
            }else if(poiCheck.isSimilar(line) && poi==null) {
                poi = allLines.get((i + 1));
            }
        }

        if(district!=null)
            results.add(district);
        if(poi!=null)
            results.add(poi);


        return results;
    }


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


    private void deleteFile() throws IOException {
        File originalPhoto = new File(currentPhotoPath);
        File faceFile = new File(getExternalFilesDir(null)+"/temp_face.jpg");

        originalPhoto.delete();
        if(originalPhoto.exists()){
            originalPhoto.getCanonicalFile().delete();
            if(originalPhoto.exists()){
                getApplicationContext().deleteFile(originalPhoto.getName());
            }
        }

        faceFile.delete();
        if(faceFile.exists()){
            faceFile.getCanonicalFile().delete();
            if(faceFile.exists()){
                getApplicationContext().deleteFile(faceFile.getName());
            }
        }
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
