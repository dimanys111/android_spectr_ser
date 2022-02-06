package com.example.dima.ser;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

public class Superficie extends SurfaceView implements SurfaceHolder.Callback {
    public boolean autofokus=false;
    SurfaceHolder mHolder;
    public Camera mCamera = null;
    int CAMERA_ID;
    WindowManager windowManager;
    Superficie(Context context,int cAMERA_ID) {
        super(context);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        CAMERA_ID=cAMERA_ID;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    public void surfaceCreated(final SurfaceHolder holder) {
        createCamera();
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera arg1) {
                    invalidar();
                }
            });
        } catch (IOException ignored) {}
    }
    public void invalidar(){
        invalidate();
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //camera.stopPreview();
        setCameraDisplayOrientation();
        mCamera.startPreview();

//            if (nachVid) {
//                if (prepareVideoRecorder()) {
//                    mediaRecorder.start();
//                } else {
//                    releaseMediaRecorder();
//                    nachVid = false;
//                }
//            }
//            else {
//                if (autofokus)
//                    mCamera.autoFocus(myAutoFocusCallback);
//                else
//                    mCamera.takePicture(null, null, myPictureCallback);
//            }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        // nothing gets drawn :(
        Paint p = new Paint(Color.RED);
        canvas.drawText("PREVIEW", canvas.getWidth() / 2,
                canvas.getHeight() / 2, p);
    }

    private void  createCamera() {
        releaseCamera();
        try {
            mCamera = Camera.open(CAMERA_ID);
        } catch (RuntimeException e) {
            try {
                if (CAMERA_ID == 0)
                    mCamera = Camera.open(1);
                else
                    mCamera = Camera.open(0);
            } catch (RuntimeException ee) {
                mCamera = null;
            }
        }
    }


    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    void setCameraDisplayOrientation() {
        // определяем насколько повернут экран от нормального положения
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }


        mCamera.setDisplayOrientation(result);

        Camera.Parameters parameters = mCamera.getParameters();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            parameters.setRotation(270);

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

        List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance != null && whiteBalance.contains(Camera.Parameters.WHITE_BALANCE_AUTO))
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            autofokus=true;
        }
        else
        {
            autofokus=false;
        }

        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        if (sizes != null && sizes.size() > 0)
        {
            Camera.Size size = sizes.get(0);
            parameters.setPictureSize(size.width, size.height);
        }

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        if (previewSizes != null)
        {
            Camera.Size previewSize = previewSizes.get(previewSizes.size() - 1);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }

        mCamera.setParameters(parameters);

        mCamera.enableShutterSound(false);
    }
}