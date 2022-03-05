package com.i2hammad.autocropandroid;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.threshold;
import static java.lang.Math.pow;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Scanner {
    Mat srcBitmap;

    int resizeThreshold = 500;
    float resizeScale = 1.0f;

    boolean canny = true;

    boolean isHisEqual = false;


    public Scanner(Mat srcBitmap, boolean canny) {
        this.srcBitmap = srcBitmap;
        this.canny = canny;
    }


    List<Point> scanPoint() {
        List<Point> result = new ArrayList<>();
        int[] cannyValue = new int[]{100, 150, 300};
        int[] blurValue = new int[]{3, 7, 11, 15};

        Mat image = resizeImage();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {

                Mat scanImage = preprocessedImage(image, cannyValue[i], blurValue[j]);
                List<MatOfPoint> contours = new ArrayList<>();

                Mat hierarchy = new Mat();

                Imgproc.findContours(scanImage, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);


                Collections.sort(contours, new Comparator<MatOfPoint>() {
                    @Override
                    public int compare(MatOfPoint v1, MatOfPoint v2) {
                        double v1Area = Imgproc.contourArea(v1);
                        double v2Area = Imgproc.contourArea(v2);
                        return Double.compare(v1Area, v2Area);
                    }
                });


                if (contours.size() > 0) {

                    MatOfPoint contour = contours.get(0);

                    MatOfPoint2f contourFloat = toMatOfPointFloat(contour);


                    double arc = Imgproc.arcLength(contourFloat, true);

                    MatOfPoint2f outDP = new MatOfPoint2f();

                    Imgproc.approxPolyDP(contourFloat, outDP, 0.01 * arc, true);
                    //筛选去除相近的点

                    List<Point> selectedPoints = selectPoints(outDP);

                    if (selectedPoints.size() != 4) {
                        //如果筛选出来之后不是四边形
                        continue;
                    } else {
                        double widthMin = selectedPoints.get(0).x;
                        double widthMax = selectedPoints.get(0).x;
                        double heightMin = selectedPoints.get(0).y;
                        double heightMax = selectedPoints.get(0).y;
                        for (int k = 0; k < 4; k++) {
                            if (selectedPoints.get(k).x < widthMin) {
                                widthMin = selectedPoints.get(k).x;
                            }
                            if (selectedPoints.get(k).x > widthMax) {
                                widthMax = selectedPoints.get(k).x;
                            }
                            if (selectedPoints.get(k).y < heightMin) {
                                heightMin = selectedPoints.get(k).y;
                            }
                            if (selectedPoints.get(k).y > heightMax) {
                                heightMax = selectedPoints.get(k).y;
                            }
                        }
                        //选择区域外围矩形面积
                        double selectArea = (widthMax - widthMin) * (heightMax - heightMin);
                        int imageArea = scanImage.cols() * scanImage.rows();
                        if (selectArea < (imageArea / 20)) {
                            result.clear();
                            //筛选出来的区域太小
                            continue;
                        } else {


                            result = selectedPoints;
                            if (result.size() != 4) {
                                Point[] p = new Point[4];


                                p[0] = new Point(0, 0);
                                p[1] = new Point(image.cols(), 0);
                                p[2] = new Point(image.cols(), image.rows());
                                p[3] = new Point(0, image.rows());
                                result.add(p[0]);
                                result.add(p[1]);
                                result.add(p[2]);
                                result.add(p[3]);
                            }
                            for (Point p : result) {
                                p.x *= resizeScale;
                                p.y *= resizeScale;
                            }
                            // 按左上，右上，右下，左下排序
                            return sortPointClockwise(result);
                        }
                    }
                }
            }
        }

        if (!isHisEqual) {
            isHisEqual = true;
            return scanPoint();
        }
        if (result.size() != 4) {
            Point[] p = new Point[4];
            p[0] = new Point(0, 0);
            p[1] = new Point(image.cols(), 0);
            p[2] = new Point(image.cols(), image.rows());
            p[3] = new Point(0, image.rows());
            result.add(p[0]);
            result.add(p[1]);
            result.add(p[2]);
            result.add(p[3]);
        }

        for (Point p : result) {
            p.x *= resizeScale;
            p.y *= resizeScale;
        }

        return sortPointClockwise(result);
    }

    private List<Point> sortPointClockwise(List<Point> points) {
        if (points.size() != 4) {
            return points;
        }
        Point unFoundPoint = new Point();
        Point[] result = {unFoundPoint, unFoundPoint, unFoundPoint, unFoundPoint};


        double minDistance = -1;
        for (Point point : points) {
            double distance = point.x * point.x + point.y * point.y;
            if (minDistance == -1 || distance < minDistance) {
                result[0] = point;
                minDistance = distance;
            }
        }

        if (result[0] != unFoundPoint) {
            Point leftTop = result[0];
            points.remove(leftTop);
            if ((pointSideLine(leftTop, points.get(0), points.get(1)) * pointSideLine(leftTop, points.get(0), points.get(2))) < 0) {
                result[2] = points.get(0);
            } else if ((pointSideLine(leftTop, points.get(1), points.get(0)) * pointSideLine(leftTop, points.get(1), points.get(2))) < 0) {
                result[2] = points.get(1);
            } else if ((pointSideLine(leftTop, points.get(2), points.get(0)) * pointSideLine(leftTop, points.get(2), points.get(1))) < 0) {
                result[2] = points.get(2);
            }
        }
        if (result[0] != unFoundPoint && result[1] != unFoundPoint && result[2] != unFoundPoint && result[3] != unFoundPoint) {
            return Arrays.asList(result);
        }
        return points;
    }

    double pointSideLine(Point lineP1, Point lineP2, Point point) {
        double x1 = lineP1.x;
        double y1 = lineP1.y;
        double x2 = lineP2.x;
        double y2 = lineP2.y;
        double x = point.x;
        double y = point.y;
        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
    }


    List<Point> selectPoints(MatOfPoint2f outDP) {

        List<Point> res = outDP.toList();
        List<Point> points = new ArrayList<>(res);

        if (points.size() > 4) {
            Point p = points.get(0);
            double minX = p.x;
            double maxX = p.x;
            double minY = p.y;
            double maxY = p.y;
            for (int i = 1; i < points.size(); i++) {
                if (points.get(i).x < minX) {
                    minX = points.get(i).x;
                }
                if (points.get(i).x > maxX) {
                    maxX = points.get(i).x;
                }
                if (points.get(i).y < minY) {
                    minY = points.get(i).y;
                }
                if (points.get(i).y > maxY) {
                    maxY = points.get(i).y;
                }
            }


            Point center = new Point((minX + maxX) / 2, (minY + maxY) / 2);
            //分别得出左上，左下，右上，右下四堆中的结果点
            Point p0 = choosePoint(center, points, 0);
            Point p1 = choosePoint(center, points, 1);
            Point p2 = choosePoint(center, points, 2);
            Point p3 = choosePoint(center, points, 3);
            points.clear();

            if (!(p0.x == 0 && p0.y == 0)) {
                points.add(p0);
            }
            if (!(p1.x == 0 && p1.y == 0)) {
                points.add(p1);
            }
            if (!(p2.x == 0 && p2.y == 0)) {
                points.add(p2);
            }
            if (!(p3.x == 0 && p3.y == 0)) {
                points.add(p3);
            }
        }
        return points;
    }


    Point choosePoint(Point center, List<Point> points, int type) {
        int index = -1;
        int minDis = 0;
        //四个堆都是选择距离中心点较远的点
        if (type == 0) {
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).x < center.x && points.get(i).y < center.y) {

                    int dis = (int) Math.sqrt(pow((points.get(i).x - center.x), 2) + pow((points.get(i).y - center.y), 2));
                    if (dis > minDis) {
                        index = i;
                        minDis = dis;
                    }
                }
            }
        } else if (type == 1) {
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).x < center.x && points.get(i).y > center.y) {
                    int dis = (int) (Math.sqrt(pow((points.get(i).x - center.x), 2) + pow((points.get(i).y - center.y), 2)));
                    if (dis > minDis) {
                        index = i;
                        minDis = dis;
                    }
                }
            }
        } else if (type == 2) {
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).x > center.x && points.get(i).y < center.y) {
                    int dis = (int) (Math.sqrt(pow((points.get(i).x - center.x), 2) + pow((points.get(i).y - center.y), 2)));
                    if (dis > minDis) {
                        index = i;
                        minDis = dis;
                    }
                }
            }

        } else if (type == 3) {
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).x > center.x && points.get(i).y > center.y) {
                    int dis = (int) (Math.sqrt(pow((points.get(i).x - center.x), 2) + pow((points.get(i).y - center.y), 2)));
                    if (dis > minDis) {
                        index = i;
                        minDis = dis;
                    }
                }
            }
        }

        if (index != -1) {
            return new Point(points.get(index).x, points.get(index).y);
        }
        return new Point(0, 0);
    }


    Mat preprocessedImage(Mat srcBitmap, int cannyValue, int blurValue) {
        Mat grayMat = new Mat();
        Imgproc.cvtColor(srcBitmap, grayMat, COLOR_BGR2GRAY);

        if (!canny) {
            return grayMat;
        }
        if (isHisEqual) {
            Imgproc.equalizeHist(grayMat, grayMat);
        }

        Mat blurMat = new Mat();

        Imgproc.GaussianBlur(grayMat, blurMat, new Size(blurValue, blurValue), 0);

        Mat cannyMat = new Mat();
        Canny(blurMat, cannyMat, 50, cannyValue, 3);
        Mat thresholdMat = new Mat();
        threshold(cannyMat, thresholdMat, 0, 255, THRESH_OTSU);
        return thresholdMat;
    }


    Mat resizeImage() {

        int width = srcBitmap.cols();
        int height = srcBitmap.rows();
        int maxSize = width > height ? width : height;
        if (maxSize > resizeThreshold) {
            resizeScale = 1.0f * maxSize / resizeThreshold;
            width = (int) (width / resizeScale);
            height = (int) (height / resizeScale);
            Size size = new Size(width, height);
            Mat resizedBitmap = new Mat(size, CV_8UC3);
            Imgproc.resize(srcBitmap, resizedBitmap, size);
            return resizedBitmap;
        }
        return srcBitmap;
    }

    public static MatOfPoint2f toMatOfPointFloat(MatOfPoint mat) {
        MatOfPoint2f matFloat = new MatOfPoint2f();
        mat.convertTo(matFloat, CvType.CV_32FC2);
        return matFloat;
    }
}
