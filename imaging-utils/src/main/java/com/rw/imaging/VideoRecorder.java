package com.rw.imaging;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/**
 * Intavu
 * <p/>
 * Created by ravindu on 28/07/16.
 * Copyright © 2016 Vortilla. All rights reserved.
 */
@SuppressWarnings("deprecation")
public class VideoRecorder
{
    private MediaRecorder mRecorder = null;
    private boolean mRecordVideoFailed = false;
    private boolean mIsRecording = false;

    private static VideoRecorder sInstance = null;


    /////////////////////////////////////////////////////////////////////////
    ///////// PUBLIC API ///////////////////////////////////////////////////

    public static void startRecording(String fileName, int rotationHint, int maxDurationMillis)
    {
        getInstance()._startRecording(CameraController.getCameraForRecording(), fileName, rotationHint, maxDurationMillis);
    }

    public static void startRecording(String fileName, int rotationHint, int maxDurationMillis, CamcorderProfile profile)
    {
        getInstance()._startRecording(CameraController.getCameraForRecording(), fileName, rotationHint, maxDurationMillis, profile);
    }

    public static void stopRecording()
    {
        getInstance()._stopRecording();
    }

    public static boolean isRecording()
    {
        return getInstance()._isRecording();
    }

    public static boolean isRecordingFailed()
    {
        return getInstance()._isRecordingFailed();
    }


    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////



    private static VideoRecorder getInstance()
    {
        if(sInstance == null)
            sInstance = new VideoRecorder();

        return sInstance;
    }

    private boolean _isRecordingFailed()
    {
        return mRecordVideoFailed;
    }

    private synchronized void _startRecording(Camera camera, String outPutFile, int rotationHint, int maxdurationMillis, CamcorderProfile profile)
    {
        /**
         * IMPORTANT - Do not change the order of the steps in this function. Code
         * taken from the google camcoder sample
         *
         */

        Log.d(ImagingUtils.TAG, "start recording video");

        mIsRecording = true;

        // Step 1: Unlock and set camera to MediaRecorder
        mRecorder = new MediaRecorder();
        camera.unlock();

        mRecorder.setCamera(camera);


        // Step 2: Set sources
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mRecorder.setProfile(profile);

        // Step 4: Set rotation and max duration

        //clckwise rotation for composition matrix as opposed to the 90degree anti-clockwise
        //rotation for the camera viewfinder
        //NOTE : This only works on some media players. Eg: works on VLC windows but not on VLC Linux
        mRecorder.setOrientationHint(rotationHint);
        mRecorder.setMaxDuration(maxdurationMillis+1000);


        // Step 5 : set the file
        mRecorder.setOutputFile(outPutFile);

        // Step 6 : Prepare
        try
        {
            mRecorder.prepare();
        }
        catch (IOException e)
        {
            //Log.e("MediaRecorder prepare failed");
            e.printStackTrace();
        }
        mRecorder.start();
    }

    private synchronized void _startRecording(Camera camera, String outPutFile, int rotationHint, int maxdurationMillis)
    {
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        profile.videoFrameWidth = 640;
        profile.videoFrameHeight = 480;
        profile.audioBitRate = 128;
        profile.audioCodec = MediaRecorder.AudioEncoder.AAC;

        _startRecording(camera, outPutFile, rotationHint, maxdurationMillis, profile);
    }

    private synchronized void _stopRecording()
    {
        /**Note from SourceCode:
         * Note that a RuntimeException is intentionally thrown to the application,
         * if no valid audio/video data has been received when stop() is called.
         *  This happens if stop() is called immediately after start().
         *  The failure lets the application take action accordingly to clean up the output file
         *  (delete the output file, for instance), since the output file is not properly
         *  constructed when this happens.
         */
        try
        {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            Log.d(ImagingUtils.TAG, "stopped mediarecorder");
            mRecordVideoFailed = false;
        }
        catch (RuntimeException re)
        {
            Log.e(ImagingUtils.TAG, "Video file creation failed");
            re.printStackTrace();
            mRecordVideoFailed = true;
        }

        mIsRecording = false;
    }

    private boolean _isRecording()
    {
        return mIsRecording;
    }
}
