package boluo.videoclips.commands;

import boluo.videoclips.FrameGrabStarter;
import boluo.videoclips.FrameRecordStarter;
import boluo.videoclips.OpChain;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Size;

@Setter
@Getter
public class ResizeOp extends Op implements FrameRecordStarter, FrameGrabStarter {

    @Min(value = 426, message = "min width is 426")
    @Max(value = 4320, message = "max width is 4320")
    private int width;
    @Min(value = 240, message = "min height is 240")
    @Max(value = 2160, message = "max height is 2160")
    private int height;

    @Override
    public void start() {

    }

    @Override
    public void afterStart(FrameGrabber grabber) {
        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();
        if(this.width > width) {
            this.width = width;
        }
        if(this.height > height) {
            this.height = height;
        }
    }

    @Override
    public void beforeStart(FrameRecorder recorder) {
        recorder.setImageWidth(width);
        recorder.setImageHeight(height);
    }

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        if(frame.image != null) {
            Mat mat = Java2DFrameUtils.toMat(frame);
            Mat resizeMat = new Mat();
            opencv_imgproc.resize(mat, resizeMat, new Size(width, height));
            frame = Java2DFrameUtils.toFrame(resizeMat);
        }
        chain.doFilter(context, frame);
    }


}
