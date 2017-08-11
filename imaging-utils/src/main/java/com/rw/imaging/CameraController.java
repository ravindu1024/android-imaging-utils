package com.rw.imaging;

import android.Manifest;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

/**
 * CameraUtils
 * <p>
 * Created by ravindu on 15/08/16.
 * Copyright © 2016 Vortilla. All rights reserved.
 */
@SuppressWarnings("deprecation")
public class CameraController implements TextureView.SurfaceTextureListener, Camera.PreviewCallback
{
    private static CameraController sInstance = null;

    private PreviewCallback mCallback = null;
    private CameraState mState = CameraState.Idle;
    private Camera mCamera = null;
    private TextureView mTargetTexture = null;
    private int mCameraId;
    private PreviewSize mCurrentPreviewSize;
    private final Boolean mCameraLock = true;


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////// PUBLIC API //////////////////////////////////////////////////////////////////////////

    @RequiresPermission(Manifest.permission.CAMERA)
    public static boolean connect(CameraType type, int rotation)
    {
        return getInstance()._connect(type, rotation);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public static boolean connect(CameraType type)
    {
        return getInstance()._connect(type, 0);
    }

    public static void disconnect()
    {
        getInstance()._disconnect();
    }

    public static void startPreview(TextureView preview, PreviewSize size)
    {
        getInstance()._startPreview(preview, size);
    }

    public static void startPreview(TextureView preview)
    {
        getInstance()._startPreview(preview, new PreviewSize(640, 480));
    }


    public static void setPreviewCallback(PreviewCallback callback)
    {
        getInstance()._setPreviewCallback(callback);
    }

    public static void stopPreview()
    {
        getInstance()._stopPreview();
    }

    public static CameraState getCameraState()
    {
        return getInstance().mState;
    }

    public static int getCurrentCameraId()
    {
        return getInstance().mCameraId;
    }

    public static PreviewSize getBestPreviewSize(int width, int height)
    {
        return getInstance()._getBestPreviewSize(width, height);
    }

    public static Camera.Parameters getCameraParams()
    {
        return getInstance().mCamera.getParameters();
    }

    public static void setCameraParams(Camera.Parameters params)
    {
        getInstance().mCamera.setParameters(params);
    }

    public static void startFaceDetection(Camera.FaceDetectionListener listener)
    {
        getInstance()._startFaceDetection(listener);
    }

    public static void stopFaceDetection()
    {
        getInstance()._stopFaceDetection();
    }

    public static void autoFocusAsync(Camera.AutoFocusCallback autoFocusCallback)
    {
        getInstance().mCamera.autoFocus(autoFocusCallback);
    }

    public static boolean isPreviewOn()
    {
        return getInstance()._isPreviewOn();
    }


    @SuppressWarnings("WeakerAccess")
    public static class Face
    {
        public RectF rect = new RectF();
        public float[] leftEye = new float[2];
        public float[] rightEye = new float[2];
        public float[] mouth = new float[2];

        private Matrix conversionMatrix = new Matrix();

        public void iniaialize(int targetWidth, int targetHeight, boolean mirror)
        {
            conversionMatrix.setScale(-1, 1);
            conversionMatrix.postRotate(mirror ? 90 : 0);
            conversionMatrix.postScale(targetWidth / 2000f, targetHeight / 2000f);
            conversionMatrix.postTranslate(targetWidth / 2f, targetHeight / 2f);
        }

        public void calculate(Camera.Face face, boolean mirror)
        {
            conversionMatrix.postRotate(mirror ? 90 : 0);

            rect.top = face.rect.top;
            rect.left = face.rect.left;
            rect.right = face.rect.right;
            rect.bottom = face.rect.bottom;

            conversionMatrix.mapRect(rect);

            if (face.leftEye != null)
            {
                leftEye[0] = face.leftEye.x;
                leftEye[1] = face.leftEye.y;

                conversionMatrix.mapPoints(leftEye);
            }

            if (face.rightEye != null)
            {
                rightEye[0] = face.rightEye.x;
                rightEye[1] = face.rightEye.y;

                conversionMatrix.mapPoints(rightEye);
            }

            if (face.mouth != null)
            {
                mouth[0] = face.mouth.x;
                mouth[1] = face.mouth.y;

                conversionMatrix.mapPoints(mouth);
            }

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private CameraController()
    {
    }

    //only used by the mediarecorder class
    static Camera getCameraForRecording()
    {
        return getInstance().mCamera;
    }

    private static CameraController getInstance()
    {
        if (sInstance == null)
            sInstance = new CameraController();

        return sInstance;
    }

    private boolean _connect(CameraType type, int rotation)
    {
        synchronized (mCameraLock)
        {
            boolean ret = true;

            Log.d(ImagingUtils.TAG, "Camera connect  to : " + type);

            if (mState == CameraState.Idle)
            {
                if (type == CameraType.Default)
                {
                    mCameraId = 0;
                    mCamera = Camera.open();
                }
                else if (type == CameraType.Front)
                {
                    mCameraId = getFrontCamId();
                    if (mCameraId != -1)
                    {
                        mCamera = Camera.open(mCameraId);
                    }
                    else
                    {
                        ret = false;
                        Log.e(ImagingUtils.TAG, "Device does not have a front camera");
                    }
                }

                if (ret)
                {
                    mCamera.setDisplayOrientation(rotation);
                    mState = CameraState.Opened;
                    Log.d(ImagingUtils.TAG, "Camera opened");
                }

            }


            return ret;
        }
    }

    private void _disconnect()
    {
        synchronized (mCameraLock)
        {
            if (mCamera != null)
            {
                _stopPreview();

                mCamera.release();
                mCamera = null;

                mState = CameraState.Idle;

                Log.d(ImagingUtils.TAG, "Camera released");
            }
        }
    }

    private void _startPreview(TextureView preview, PreviewSize size)
    {
        if (mState == CameraState.Opened)
        {
            mState = CameraState.PreviewStarted;

            mCurrentPreviewSize = size;

            preview.setSurfaceTextureListener(this);

            synchronized (mCameraLock)
            {

                Camera.Parameters p = mCamera.getParameters();
                p.setPreviewSize(size.width, size.height);

                mCamera.setParameters(p);
            }

            mTargetTexture = preview;

            Log.d(ImagingUtils.TAG, "Camera preview is avalable: " + preview.isAvailable());

            if (preview.isAvailable())
            {
                onSurfaceTextureAvailable(mTargetTexture.getSurfaceTexture(), mTargetTexture.getWidth(), mTargetTexture.getHeight());
            }
        }

    }

    private void _stopPreview()
    {
        synchronized (mCameraLock)
        {
            if (mCamera != null)
            {
                if (mTargetTexture != null)
                    mTargetTexture.setSurfaceTextureListener(null);

                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                try
                {
                    mCamera.setPreviewDisplay(null);
                    mCamera.setPreviewTexture(null);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                mState = CameraState.Opened;
            }
        }
    }

    private boolean _isPreviewOn()
    {
        synchronized (mCameraLock)
        {
            return (mCamera != null && mState == CameraState.PreviewStarted);
        }
    }

    private int getFrontCamId()
    {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++)
        {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No front-facing camera found
    }

    private void _setPreviewCallback(final PreviewCallback callback)
    {
        mCallback = callback;

        /**
         * this is to forcefully set the preview callback everytime the user requests it
         */
        if (mCamera != null)
        {
            mCamera.setPreviewCallback(this);
        }
    }


    private PreviewSize _getBestPreviewSize(int width, int height)
    {
        float aspectRatio = (float) width / (float) height;

        PreviewSize preview = new PreviewSize(640, 480);
        Log.d("IMG", "preview aspect to find: " + Float.toString(aspectRatio));
        float min = -1;

        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();

        for (Camera.Size s : sizes)
        {
            float ar = (float) s.width / (float) s.height;

            Log.d(ImagingUtils.TAG, "preview size: " + s.width + "x" + s.height + ", aspect ratio: " + Float.toString(ar));

            if (min == -1 || Math.abs(aspectRatio - min) > Math.abs(aspectRatio - ar) || (Math.abs(min - ar) < 0.001f && s.width > preview.width))
            {
                min = ar;
                preview = new PreviewSize(s.width, s.height);
                Log.d(ImagingUtils.TAG, "selecting size: " + s.width + "x" + s.height);
            }
        }

        Log.d(ImagingUtils.TAG, "Best preview size: " + preview.width + "x" + preview.height);

        return preview;
    }

    private void _startFaceDetection(Camera.FaceDetectionListener listener)
    {
        Log.d(ImagingUtils.TAG, "Begin face detection");
        mCamera.setFaceDetectionListener(listener);
        mCamera.startFaceDetection();
    }

    private void _stopFaceDetection()
    {
        mCamera.stopFaceDetection();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1)
    {
        Log.d(ImagingUtils.TAG, "Camera on surface texture available");
        synchronized (mCameraLock)
        {
            if (mTargetTexture.getSurfaceTexture() == surfaceTexture && mState == CameraState.PreviewStarted)
            {
                try
                {
                    Log.d(ImagingUtils.TAG, "Camera starting preview");
                    mCamera.setPreviewTexture(surfaceTexture);
                    mCamera.setPreviewCallback(getInstance());
                    mCamera.startPreview();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1)
    {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
    {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
    {

    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera)
    {
        if (mCallback != null)
            mCallback.onCameraFrame(bytes, mCurrentPreviewSize);
    }

    public enum CameraType
    {
        Default, Front
    }

    public enum CameraState
    {
        Idle, Opened, PreviewStarted
    }

    public static class PreviewSize
    {
        public final int width;
        public final int height;

        public PreviewSize(int w, int h)
        {
            width = w;
            height = h;
        }
    }

    public interface PreviewCallback
    {
        void onCameraFrame(byte[] frame, PreviewSize previewSize);
    }
}
