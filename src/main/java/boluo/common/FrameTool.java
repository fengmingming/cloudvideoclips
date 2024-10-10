package boluo.common;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

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

}
