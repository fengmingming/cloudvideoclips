package boluo.videoclips.commands;

import boluo.videoclips.FrameGrabStarter;
import boluo.videoclips.OpChain;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

public class ChromakeyOp extends Op implements FrameGrabStarter {


    @Setter @Getter @Min(value = 0,message = "the minimum of startTime is 0") private long startTime;
    @Setter @Getter private long endTime = Long.MAX_VALUE;
    @Setter @Getter
    @Min(value = 0, message = "the minimum of hmin is 0") @Max(value = 255, message = "the maximum of hmin is 255")
    private double hmin = 35;
    @Setter @Getter
    @Min(value = 0,message = "the minimum of smin is 0") @Max(value = 255,message = "the maximum of smin is 255")
    private double smin = 43;
    @Setter @Getter
    @Min(value = 0,message = "the vmin of vmin is 0") @Max(value = 255,message = "the maximum of vmin is 255")
    private double vmin = 46;
    @Setter @Getter
    @Min(value = 0,message = "the minimum of hmax is 0") @Max(value = 255,message = "the maximum of hmax is 255")
    private double hmax = 77;
    @Setter @Getter
    @Min(value = 0,message = "the minimum of smax is 0") @Max(value = 255,message = "the maximum of smax is 255")
    private double smax = 255;
    @Setter @Getter
    @Min(value = 0,message = "the minimum of vmax is 0") @Max(value = 255,message = "the maximum of vmax is 255")
    private double vmax = 255;
    private Mat lower;
    private Mat upper;
    @Setter @Getter
    private Integer width;
    @Setter @Getter
    private Integer height;

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {

    }

    @Override
    public void afterStart(FrameGrabber grabber) {
        if(this.width == null) {
            this.width = grabber.getImageWidth();
        }
        if(this.height == null) {
            this.height = grabber.getImageHeight();
        }
        Scalar scalar = new Scalar(hmin, smin, vmin, (double)0);
        this.lower = new Mat(this.width, this.height, opencv_core.CV_8UC3);
    }

}
