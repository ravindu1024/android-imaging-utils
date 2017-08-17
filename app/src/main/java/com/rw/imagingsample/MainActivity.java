package com.rw.imagingsample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import com.rw.imaging.CameraController;
import com.rw.imaging.ImagingUtils;
import com.rw.imagingsample.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity implements CameraController.PreviewCallback
{
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mBinding.frontCam.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                stopCamera();
                startCamera(CameraController.CameraType.Front, 90);
            }
        });

        mBinding.backCam.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                stopCamera();
                startCamera(CameraController.CameraType.Default, 90);
            }
        });

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        else
        {
            startCamera(CameraController.CameraType.Front, 90);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
//    {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
//            startCamera();
//    }

    private void startCamera(CameraController.CameraType type, int rotation)
    {
        Log.d("IMG", "opencv version: " + ImagingUtils.getVersion());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        else
        {
            CameraController.connect(type, rotation);

            CameraController.PreviewSize p = new CameraController.PreviewSize(640, 480);// CameraController.getBestPreviewSize(640, 480);

            Log.d("IMG", "best preview: " + p.width + "x" + p.width);

            CameraController.setPreviewCallback(this);

            CameraController.startPreview(mBinding.previewMain, p);
        }
    }

    private void stopCamera()
    {
        CameraController.stopPreview();
        CameraController.disconnect();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        stopCamera();
    }

    @Override
    public void onCameraFrame(byte[] frame, CameraController.PreviewSize previewSize)
    {
        Canvas c = mBinding.previewCustom.lockCanvas();

        long t = System.currentTimeMillis();

        ImagingUtils.PreviewResizeParams p = new ImagingUtils.PreviewResizeParams.Builder()
                .setTargetSize(c.getWidth(), c.getHeight())
                .setBounds(0, (previewSize.width-previewSize.height)/2, previewSize.height, previewSize.width - (previewSize.width-previewSize.height)/2)
                .setRotation(ImagingUtils.PreviewResizeParams.Rotation.Clockwise_90)
                .create();

        Bitmap b = ImagingUtils.rotateCropAndResizePreview(frame, previewSize.width, previewSize.height, p);
        c.drawBitmap(b, 0, 0, null);


        Log.d("IMG", "processing time: " + (System.currentTimeMillis() - t));

        mBinding.previewCustom.unlockCanvasAndPost(c);
    }
}
