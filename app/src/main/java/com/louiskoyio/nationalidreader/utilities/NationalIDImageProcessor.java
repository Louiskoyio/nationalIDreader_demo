package com.louiskoyio.nationalidreader.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import android.util.SparseArray;

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
import com.louiskoyio.nationalidreader.MyString;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NationalIDImageProcessor{

    private static final String TAG = "NationalIDProcessor";
    public File image;
    public Bitmap mBitmap,mFaceBitmap;
    public Context mContext;
    private String currentPhotoPath;
    public String predictedName;
    public String predictedIDNumber;
    public List<String> processedResults;
    private ProgressDialog mProgressDialog;

    public NationalIDImageProcessor(Context context, File image) {

        this.image = image;
        this.mContext = context;
        this.mBitmap = getBitmap(image);
        this.currentPhotoPath = image.getAbsolutePath();
        this.mProgressDialog = new ProgressDialog(context);
        this.processedResults = new ArrayList<>();
    }


    public List<String> run() {

        // correct the image orientation first
        try {
            mBitmap = correctImageOrientation(mBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }


        /** begin processing text*/


        InputImage image = InputImage.fromBitmap(mBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {
                            
                            public void onSuccess(Text texts) {
                                processedResults = getNameAndIDNumber(texts);

                                List<Text.TextBlock> blocks = texts.getTextBlocks();
                                if (blocks.size() == 0) {
                                    mBitmap = correctedImage(mBitmap);

                                }

                                System.out.println("\nName - "+predictedName);
                                System.out.println("\nNumber - "+predictedIDNumber);

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });


        /** end processing text */

        /** begin processing face */


        FaceDetector detector = new FaceDetector.Builder(mContext)
                .setProminentFaceOnly(true)
                .build();
        Bitmap source = mBitmap;
        Frame outputFrame = new Frame.Builder().setBitmap(source).build();
        detector.detect(outputFrame);
        SparseArray<Face> faces = detector.detect(outputFrame);


        mFaceBitmap = getFace(faces);

        /** end processing face */

        return processedResults;

    }

    public String getPredictedName() {
        return predictedName;
    }

    public String getPredictedIDNumber() {
        return predictedIDNumber;
    }

    public Bitmap getFaceBitmap() {
        return mFaceBitmap;
    }

    public Bitmap getOriginalBitmapResized(){
        return mBitmap;
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

    private Bitmap correctImageOrientation(Bitmap bitmap) throws IOException {
        Matrix matrix = new Matrix();

        ExifInterface exif = new ExifInterface(currentPhotoPath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);


        switch (orientation) {
            case ExifInterface.ORIENTATION_TRANSPOSE:
                Log.d(TAG, "rotateBitmap: transpose");
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                Log.d(TAG, "rotateBitmap: normal.");
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                Log.d(TAG, "rotateBitmap: flip horizontal");
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                Log.d(TAG, "rotateBitmap: rotate 180");
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                Log.d(TAG, "rotateBitmap: rotate vertical");
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                Log.d(TAG, "rotateBitmap: rotate 90");
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                Log.d(TAG, "rotateBitmap: transverse");
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                Log.d(TAG, "rotateBitmap: rotate 270");
                matrix.setRotate(-90);
                break;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }


    }

    public Bitmap correctedImage(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.setRotate(-90);
        Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return bmRotated;

    }

    public List<String> getNameAndIDNumber(Text texts) {

        List<String> allLines = new ArrayList<>();
        List<String> allValidLines = new ArrayList<>();
        List<String> allInvalidLines = new ArrayList<>();
        List<String> predictedResults = new ArrayList<>();
        List<String> allNumbers = new ArrayList<>();
        List<String> validNumbers = new ArrayList<>();

        String allLinesString="";
        String allValidLinesString="";
        String allInvalidLinesString ="";

        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            predictedName = "";
            predictedIDNumber = "";
        }



        List<String> invalidLines  = new ArrayList<>();

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
                    if(containsDot(word))
                        allNumbers.add(word);

                    //give the line a temp id
                    String lineId = i+"-"+j;

                    // check if word is valid
                    if(isWordValid(word) && !invalidLines.contains(lineId)){
                        String lineToBeAdded = lines.get(j).getText();
                        if(!allValidLines.contains(lineToBeAdded))
                            allValidLines.add(lineToBeAdded);
                    }else if(isWordValid(word) && invalidLines.contains(lineId)){
                        allValidLines.add(word);
                    }if(!isWordValid(word) && !invalidLines.contains(lineId)){
                        invalidLines.add(i+"-"+j);
                        allInvalidLines.add(lines.get(j).getText());
                    }

                }
            }
        }



        for(String line: allLines){
            allLinesString = allLinesString + line + "\n";
        }

        int linesWithNumbers = 0;
        int linesWithWords = 0;
        for(String line: allValidLines){


            if(containsNumber(line)){
                String newLine = cleanupNumber(line);

                if(newLine.length()<=8 && linesWithNumbers<1 && !containsDot(line)) {
                    allValidLinesString = allValidLinesString + newLine + "\n";
                    predictedResults.add(newLine);
                    linesWithNumbers++;
                }
            }else {
                if(linesWithWords<1 && !allInvalidLines.contains(line) && lineWithMoreThanTwoWords(line)  ) {
                    allValidLinesString = allValidLinesString + line + "\n";
                    predictedResults.add(line);

                    linesWithWords++;
                }
            }
        }
        for(String line: allInvalidLines){
            allInvalidLinesString = allInvalidLinesString + line + "\n";
        }

        mProgressDialog.setMessage("Processing text: 6/7");
        if(allLines.size()>0) {
            int indexOfIDNumber=0,indexOfName=0;

            for(String prediction: predictedResults){
                if(StringUtils.isNumeric(prediction))
                    indexOfIDNumber  = predictedResults.indexOf(prediction);
                else
                    indexOfName = predictedResults.indexOf(prediction);
            }

            mProgressDialog.setMessage("Processing text: 7/7");
            if(allNumbers.size()>0) {
                validNumbers = validNumbers(allNumbers);

                if(validNumbers.size()>0) {
                    predictedIDNumber = validNumbers.get(0);
                }else {
                    predictedIDNumber = "";
                }
            }

            if(predictedResults.size()>0) {
                predictedName = predictedResults.get(indexOfName);
            }else {
                predictedName = "";
            }

            processedResults.add(predictedName);
            processedResults.add(predictedIDNumber);
/*            allInfo.setText("ALL:\n" + allLinesString);
            allValidInfo.setText("VALID:\n"+ allValidLinesString);
            removedInfo.setText("REMOVED:\n" + allInvalidLines);*/
        }

        return processedResults;

    }

    private Bitmap getFace(SparseArray<Face> faces) {
        // Task completed successfully
        if (faces.size() == 0) {
            mFaceBitmap = null;
        }else {
            Bitmap source = mBitmap;

            for (int i = 0; i < faces.size(); ++i) {
                Face face = faces.get(i);

                if(face!=null) {
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

    private void saveRecognizedFace(Bitmap faceBitmap){
        final CallbackInterface callback = new CallbackInterface() {
            
            public void done(Exception e) {
                if(e == null){
                    Log.d(TAG, "onImageSavedCallback: image saved!");
                }
                else{
                    Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                }
            }
        };

        ImageSaver imageSaver = new ImageSaver(
                faceBitmap,
                mContext.getExternalFilesDir(null),
                callback,"temp_face.jpg"
        );
        imageSaver.run();

        mFaceBitmap = faceBitmap;


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

        int similarWords=0;
        for (int i=0;i<undesiredWords.size()-1;i++)
        {
            MyString comparator = new MyString(word);
            similarToUnwantedWord = comparator.isSimilar(undesiredWords.get(i));

            if(similarToUnwantedWord)
                similarWords++;
        }



        if(similarWords>0)
            valid = false;
        else if(similarWords==0)
            valid = true;



        return valid;
    }

    
    public Boolean lineWithMoreThanTwoWords(String line) {
        Boolean hasMoreThanOneWord=false;
        String[] splitLine = line.split(" ");

        if(splitLine.length>2)
            hasMoreThanOneWord = true;

        return hasMoreThanOneWord;
    }

    
    public String cleanupNumber(String text) {
        char [] textArray = text.toCharArray();

        List<Character> removedLetters = new ArrayList<>();
        for(Character character:textArray){
            if(Character.isDigit(character))
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
        for(Character character: splitLine){
            if(character == '.')
                hasDot = true;
        }

        return hasDot;
    }

    
    public Boolean containsNumber(String line) {
        Boolean hasNumber = false;

        char[] splitLine = line.toCharArray();
        for(Character character: splitLine){
            if(Character.isDigit(character))
                hasNumber = true;
        }

        return hasNumber;
    }

    
    public List<String> validNumbers(List<String> allNumbers) {
        List<String> validNumbers = new ArrayList<>();
        List<String> filteredByContent = new ArrayList<>();
        List<String> cleanedValidNumbers = new ArrayList<>();


        //filter numbers by content - to remove dates
        for(String number: allNumbers){
            if(!containsDot(number))
                filteredByContent.add(number);
        }


        //clean all numbers - remove letters
        for(String number: filteredByContent){
            String cleanedNumber = cleanupNumber(number);
            cleanedValidNumbers.add(cleanedNumber);
        }


        //filter numbers by length - to remove serial number
        for(String number: cleanedValidNumbers){
            if(number.length()==8)
                validNumbers.add(number);
        }

        return validNumbers;
    }



}