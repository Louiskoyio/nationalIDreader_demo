package com.louiskoyio.nationalidreader.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.louiskoyio.nationalidreader.CallbackInterface;
import com.louiskoyio.nationalidreader.MainActivity;
import com.louiskoyio.nationalidreader.MyString;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NationalIDImageProcessor extends AsyncTask<Void, String, Bitmap> {

    private static final String TAG = "NationalIDProcessor";
    public File image;
    public Bitmap mBitmap,mFaceBitmap;
    public Context mContext;
    private String currentPhotoPath;
    public String predictedName,predictedIDNumber,predictedDoB,predictedSex,predictedDistrict,predictedPoI,predictedDoI;
    public List<String> processedResults;
    private ProgressDialog mProgressDialog;
    private Boolean rotated = false;
    private List<String> undesiredWords;

    public NationalIDImageProcessor(Context context, File image) {

        this.image = image;
        this.mContext = context;
        this.mBitmap = getBitmap(image);
        this.currentPhotoPath = image.getAbsolutePath();
        this.mProgressDialog = new ProgressDialog(context);
        this.processedResults = new ArrayList<>();
    }



    @Override
    protected Bitmap doInBackground(Void... voids) {
        mProgressDialog.setMessage("Processing Image. Please wait ...");
        processImage();


        return mFaceBitmap;
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
                                                mContext.getExternalFilesDir(null),
                                                callback, "rotated_image.jpg"
                                        );
                                        imageSaver.run();

                                    }
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
                    if (containsDot(word)) {
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

            predictedSex = (getGender(allWords));
            predictedDistrict = (getPoIAndDistrict(allLines).get(0));
            predictedPoI = getPoIAndDistrict(allLines).get(1);
            predictedDoB = getDoBAndDoI(allDates).get(0);
            predictedDoI = getDoBAndDoI(allDates).get(1);

            
/*
            allInfo.setText("ALL:\n" + allLinesString);
            allValidInfo.setText("VALID:\n" + allValidLinesString);
            removedInfo.setText("REMOVED:\n" + allInvalidLines);*/

            processFace();

        }
    }
    private void processFace(){
        mProgressDialog.setMessage("Processing face...");
        FaceDetector detector = new FaceDetector.Builder(mContext)
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

        ImageSaver imageSaver = new ImageSaver(
                faceBitmap,
                mContext.getExternalFilesDir(null),
                callback, "temp_face.jpg"
        );
        imageSaver.run();

        mFaceBitmap = faceBitmap;

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
    
}