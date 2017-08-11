package com.rw.imaging;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * CameraUtils
 * <p/>
 * Created by ravindu on 15/08/16.
 * Copyright © 2016 Vortilla. All rights reserved.
 */
public class ImagingUtils
{
    protected static final String TAG = "IMGUTILS";
    static
    {
        if(!OpenCVLoader.initDebug())
            Log.e(TAG, "opencv load failed");
    }

    public static String getVersion()
    {
        return Core.VERSION;
    }


    /**
     * Resize, crop and rotate the camera preview frame
     * @param bytes preview data
     * @param width original width
     * @param height original height
     * @param params image processing parameters
     * @return
     */
    public static Bitmap rotateCropAndResizePreview(byte[] bytes, int width, int height, PreviewResizeParams params)
    {
        Size finalSize = new Size(params.newWidth, params.newHeight);
        Rect cropRect = new Rect(params.cropX, params.cropY, params.cropWidth, params.cropHeight);

        Mat rawMat = new Mat(height*3/2, width, CvType.CV_8UC1); // YUV data
        rawMat.put(0, 0, bytes);
        Mat rgbMat = new Mat(height, width, CvType.CV_8UC4); // RGBA image
        Imgproc.cvtColor(rawMat, rgbMat, Imgproc.COLOR_YUV2RGBA_NV21);

        //rotate clockwise
        Mat rotatedMat = rotateFrame(rgbMat, params.rotation);

        //crop rect from image
        Mat croppedMat = new Mat(rotatedMat, cropRect);

        //resize
        if(finalSize.area() > 0)
            Imgproc.resize(croppedMat, croppedMat, finalSize);


        Bitmap bmp = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croppedMat, bmp);

        return bmp;
    }

    private static Mat rotateFrame(Mat in, PreviewResizeParams.Rotation rotation)
    {
        Mat out = in;

        if(rotation == PreviewResizeParams.Rotation.Clockwise_90)
        {
            out = in.t();
            Core.flip(out, out, -1);
        }
        else if(rotation == PreviewResizeParams.Rotation.Clockwise_180)
        {
            out = in;
        }
        else if(rotation == PreviewResizeParams.Rotation.Clockwise_270)
        {
            out = in.t();
            Core.flip(out, out, 1);
        }
        else
        {
            Core.flip(in, out, 1);
        }

        return out;
    }

    public static class PreviewResizeParams
    {
        int cropX = 0;
        int cropY = 0;
        int cropWidth = 0;
        int cropHeight = 0;
        int newWidth = 0;
        int newHeight = 0;
        Rotation rotation = Rotation.NoRotation;

        public static class Builder
        {
            int cropX = 0;
            int cropY = 0;
            int cropWidth = 0;
            int cropHeight = 0;
            int newWidth = 0;
            int newHeight = 0;
            Rotation rotation = Rotation.NoRotation;


            /**
             * Create a new empty {@link PreviewResizeParams} object
             * @return
             */
            public PreviewResizeParams create()
            {
                return new PreviewResizeParams(this);
            }

            /**
             * Set bounds for cropping the frame
             * @param left starting pixel on left
             * @param top starting pixel on top
             * @param right ending pixel on right
             * @param bottom ending pixel at bottom
             * @return PreviewResizeParams
             */
            public Builder setBounds(int left, int top, int right, int bottom)
            {
                this.cropX = left;
                this.cropY = top;
                this.cropWidth = right - left;
                this.cropHeight = bottom - top;

                return this;
            }

            /**
             * Set new size to resize frame
             * @param width new width
             * @param height new height
             * @return PreviewResizeParams
             */
            public Builder setTargetSize(int width, int height)
            {
                this.newWidth = width;
                this.newHeight = height;

                return this;
            }

            /**
             * Set image rotation
             * @param rotation rotation amount
             * @return PreviewResizeParams
             */
            public Builder setRotation(Rotation rotation)
            {
                this.rotation = rotation;

                return this;
            }

        }


        private PreviewResizeParams(PreviewResizeParams.Builder builder)
        {
            this.cropX = builder.cropX;
            this.cropY = builder.cropY;
            this.cropWidth = builder.cropWidth;
            this.cropHeight = builder.cropHeight;
            this.newWidth = builder.newWidth;
            this.newHeight = builder.newHeight;
            this.rotation = builder.rotation;
        }

        public enum Rotation
        {
            NoRotation, Clockwise_90, Clockwise_180, Clockwise_270
        }

    }
}
