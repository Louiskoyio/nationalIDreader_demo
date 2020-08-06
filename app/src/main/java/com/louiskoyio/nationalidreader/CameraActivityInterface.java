package com.louiskoyio.nationalidreader;

import android.graphics.drawable.Drawable;

public interface CameraActivityInterface {
    void setCameraFrontFacing();

    void setCameraBackFacing();

    boolean isCameraFrontFacing();

    boolean isCameraBackFacing();

    void setFrontCameraId(String cameraId);

    void setBackCameraId(String cameraId);

    String getFrontCameraId();

    String getBackCameraId();

    void hideStatusBar();

    void showStatusBar();

    void setTrashIconSize(int width, int height);

}
