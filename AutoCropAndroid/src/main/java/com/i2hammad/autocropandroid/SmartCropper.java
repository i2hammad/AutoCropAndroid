package com.i2hammad.autocropandroid;

import static org.opencv.imgproc.Imgproc.COLOR_RGBA2BGR;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import com.i2hammad.autocropandroid.utils.CropUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;


public class SmartCropper {

    public static final String TAG  = SmartCropper.class.getName();
    private static ImageDetector sImageDetector = null;

    public static void buildImageDetector(Context context) {
        SmartCropper.buildImageDetector(context, null);


    }

    public static void buildImageDetector(Context context, String modelFile) {
        try {
            sImageDetector = new ImageDetector(context, modelFile);
            Log.e(TAG, "buildImageDetector: " );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 输入图片扫描边框顶点
     *
     * @param srcBmp 扫描图片
     * @return 返回顶点数组，以 左上，右上，右下，左下排序
     */
    public static Point[] scan(Bitmap srcBmp) {
        Log.e(TAG, "scan:" );
        if (srcBmp == null) {
            throw new IllegalArgumentException("srcBmp cannot be null");
        }
        if (sImageDetector != null) {
            Log.e(TAG, "sImageDetector not null:" );

            Bitmap bitmap = sImageDetector.detectImage(srcBmp);
            if (bitmap != null) {
                srcBmp = Bitmap.createScaledBitmap(bitmap, srcBmp.getWidth(), srcBmp.getHeight(), false);
            }
        }
        Point[] outPoints = new Point[4];
        Log.e(TAG, "outpoints:" );
        scanImage(srcBmp, outPoints, sImageDetector == null);
        return outPoints;
    }

    /**
     * 裁剪图片
     *
     * @param srcBmp     待裁剪图片
     * @param cropPoints 裁剪区域顶点，顶点坐标以图片大小为准
     * @return 返回裁剪后的图片
     */
    public static Bitmap crop(Bitmap srcBmp, Point[] cropPoints) {
        if (srcBmp == null || cropPoints == null) {
            throw new IllegalArgumentException("srcBmp and cropPoints cannot be null");
        }
        if (cropPoints.length != 4) {
            throw new IllegalArgumentException("The length of cropPoints must be 4 , and sort by leftTop, rightTop, rightBottom, leftBottom");
        }
        Point leftTop = cropPoints[0];
        Point rightTop = cropPoints[1];
        Point rightBottom = cropPoints[2];
        Point leftBottom = cropPoints[3];

        int cropWidth = (int) ((CropUtils.getPointsDistance(leftTop, rightTop)
                + CropUtils.getPointsDistance(leftBottom, rightBottom)) / 2);
        int cropHeight = (int) ((CropUtils.getPointsDistance(leftTop, leftBottom)
                + CropUtils.getPointsDistance(rightTop, rightBottom)) / 2);

        Bitmap cropBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888);
          cropImage(srcBmp, cropPoints, cropBitmap);
        return cropBitmap;
    }


//    private static native void nativeScan(Bitmap srcBitmap, Point[] outPoints, boolean canny);
//
//    private static native void nativeCrop(Bitmap srcBitmap, Point[] points, Bitmap outBitmap);


    static void scanImage(Bitmap srcBitmap, Point[] outPoints, boolean canny) {
        if (outPoints.length != 4) {
            return;
        }
        Mat srcMat = new Mat();
        Utils.bitmapToMat(srcBitmap, srcMat);

        Mat bgrData = new Mat();

        Imgproc.cvtColor(srcMat, bgrData, COLOR_RGBA2BGR);

        Scanner scanner = new Scanner(bgrData, canny);
        List<org.opencv.core.Point> scanPoints = scanner.scanPoint();
        if (scanPoints.size() == 4) {
            for (int i = 0; i < scanPoints.size(); i++) {
                org.opencv.core.Point point = scanPoints.get(i);
                Point point1 = new Point((int) point.x, (int) point.y);
                outPoints[i] = point1;
            }
        }
    }


    static void cropImage(Bitmap srcBitmap, Point[] outPoints, Bitmap outPutImage) {

    }
}
