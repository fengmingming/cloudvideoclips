package boluo.videoclips.commands;

import boluo.videoclips.OpChain;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

public class RecordOp extends Op{

    private final FrameRecorder recorder;

    public RecordOp(FrameRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        try {
            recorder.record(frame);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

}
