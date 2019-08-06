package com.chillingvan.lib.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

/**
 * Created by Chilling on 2017/5/29.
 */

public interface CameraInterface {
    void setPreview(SurfaceTexture surfaceTexture);

    void openCamera();

    void switchCamera();

    void switchCamera(int previewWidth, int previewHeight);

    boolean isOpened();

    void startPreview();

    void stopPreview();

    Camera getCamera();

    void release();
}
