package com.louiskoyio.nationalidreader.utilities;

import android.graphics.Bitmap;

import com.louiskoyio.nationalidreader.CallbackInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageSaver implements Runnable {

    /** The file we save the image into. */
    private final File mFile;

    /** Original image that was captured */

    private CallbackInterface mCallback;

    private Bitmap mBitmap;
    private String mFilename;

    public ImageSaver(Bitmap bitmap, File file, CallbackInterface callback, String filename) {
        mBitmap = bitmap;
        mFile = file;
        mFilename = filename;
        mCallback = callback;
    }

    @Override
    public void run() {
        if(mBitmap != null){
            ByteArrayOutputStream stream = null;
            byte[] imageByteArray = null;
            stream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            imageByteArray = stream.toByteArray();
            String filename = mFilename ;
            File file = new File(mFile, filename);
            // save the mirrored byte array
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(imageByteArray);
            } catch (IOException e) {
                mCallback.done(e);
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCallback.done(null);
                }
            }
        }
    }
}