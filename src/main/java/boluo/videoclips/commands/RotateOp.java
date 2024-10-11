package boluo.videoclips.commands;

import boluo.common.FrameTool;
import boluo.videoclips.FrameRecordStarter;
import boluo.videoclips.OpChain;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.opencv.opencv_core.Size;

/**
 * 画面旋转操作
 * */
public class RotateOp extends Op implements FrameRecordStarter {

    @Min(value = 0, message = "the min of startTime is 0")
    @Setter @Getter
    private long startTime;
    @Setter @Getter
    private long endTime = Long.MAX_VALUE;
    @Min(0)
    @Max(360)
    @Setter @Getter
    private int angle;
    private Size size;

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        if(frame.image != null && frame.timestamp >= startTime && frame.timestamp <= endTime) {
            frame = FrameTool.rotate(frame, angle, this.size);
        }
        chain.doFilter(context, frame);
    }

    public void afterStart(FrameRecorder recorder) {
        this.size = new Size(recorder.getImageWidth(), recorder.getImageHeight());
    }

}
