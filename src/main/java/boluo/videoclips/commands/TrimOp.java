package boluo.videoclips.commands;

import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;

/**
 * 截取视频
 * */
@Getter
@Setter
public class TrimOp extends Op {

    private long startTime = 0;
    private long endTime = Long.MAX_VALUE;

    @Override
    public Frame doFilter(OpContext context, Frame frame) {
        long timestamp = frame.timestamp;
        if(timestamp >= startTime && timestamp <= endTime) {
            return null;
        }
        return frame;
    }

}
