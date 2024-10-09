package boluo.videoclips;

import org.bytedeco.javacv.FrameRecorder;

public interface FrameRecordStarter {

    default void beforeStart(FrameRecorder recorder) {}

    default void afterStart(FrameRecorder recorder) {}

}
