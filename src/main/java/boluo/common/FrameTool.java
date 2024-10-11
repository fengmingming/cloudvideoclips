package boluo.common;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Size;

public class FrameTool {

    /**
     * 替换背景
     * */
    public static Frame replaceScreen(Frame origin, Mat background, Mat lower, Mat upper) {
        Mat s = Java2DFrameUtils.toMat(origin);
        Mat hsv = new Mat();
        opencv_imgproc.cvtColor(s, hsv, opencv_imgproc.COLOR_BGR2HSV);
        opencv_core.inRange(hsv, lower, upper, hsv);
        opencv_core.bitwise_not(hsv, hsv);
        opencv_core.add(s, background, s, hsv, -1);
        return Java2DFrameUtils.toFrame(s);
    }

    /**
     * 旋转
     * */
    public static Frame rotate(Frame origin, int angle, Size size) {
        Mat s = Java2DFrameUtils.toMat(origin);
        Point2f center = new Point2f(s.cols() / 2.0f, s.rows() / 2.0f);
        Mat rotationMat = opencv_imgproc.getRotationMatrix2D(center, angle, 1.0);
        opencv_imgproc.warpAffine(s, s, rotationMat, size);
        return Java2DFrameUtils.toFrame(s);
    }

}
