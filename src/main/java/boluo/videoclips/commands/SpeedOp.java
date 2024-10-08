package boluo.videoclips.commands;

import boluo.videoclips.FrameRecordStarter;
import boluo.videoclips.OpChain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放速度操作
 * */
public class SpeedOp extends Op implements FrameRecordStarter {

    @NotBlank
    @Pattern(regexp = "0\\.25|0\\.5|0\\.75|1|1\\.25|1\\.5|1\\.75|2|3", message = "value can only be 0.25,0.5,0.75,1,1.25,1.5,1.75,2,3")
    private @Setter @Getter String value;
    private @Setter @Getter long startTime;
    private @Setter @Getter long endTime = Long.MAX_VALUE;
    private int times;
    private int cycle;
    private List<Integer> points;
    private int index;
    private double perFrameTime;
    private long count;

    @Override
    public void afterStart(FrameRecorder recorder) {
        this.perFrameTime = (1000 * 1000 / recorder.getFrameRate());
    }

    @Override
    public void start() {
        count = 0;
        points = null;
        times = 0;
        cycle = 0;
        index = 0;
        switch (value) {
            case "0.25" -> {
                times = 4;
                cycle = 1;
            }
            case "0.5" -> {
                times = 2;
                cycle = 1;
            }
            case "0.75" -> {
                times = 4;
                cycle = 3;
            }
            case "1" -> {
                cycle = 1;
                points = List.of(0);
            }
            case "1.25" -> {
                cycle = 5;
                points = List.of(0, 1, 2, 3);
            }
            case "1.5" -> {
                cycle = 3;
                points = List.of(0, 1);
            }
            case "1.75" -> {
                cycle = 7;
                points = List.of(0, 1, 2, 3);
            }
            case "2" -> {
                cycle = 2;
                points = List.of(0);
            }
            case "3" -> {
                cycle = 3;
                points = List.of(0);
            }
            default -> throw new IllegalArgumentException("value can only be 0.25,0.5,0.75,1,1.25,1.5,1.75,2,3");
        }
    }

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        long timestamp = frame.timestamp;
        if(timestamp <= startTime || timestamp >= endTime) {
            frame.timestamp = (long)(count * perFrameTime + timestamp);
            if(frame.timestamp < 0) {
                frame.timestamp = 0;
            }
            chain.doFilter(context, frame);
            return;
        }
        if(points == null) {//慢放
            List<Frame> frames = new ArrayList<>();
            if(index < cycle) {
                frame.timestamp = (long)(count * perFrameTime + timestamp);
                frames.add(frame);
            }
            index++;
            if(index == cycle) {
                for(int i = times - cycle; i > 0; i--) {
                    Frame f = frame.clone();
                    f.timestamp = (long)(++count * perFrameTime + timestamp);
                    frames.add(f);
                }
                index = 0;
            }
            chain.doFilter(context, frames);
        }else {//加速
            if(index < cycle) {
                if(points.contains(index)) {
                    frame.timestamp = (long)(count * perFrameTime + timestamp);
                    if(frame.timestamp < 0) {
                        frame.timestamp = 0;
                    }
                    chain.doFilter(context, frame);
                }else {
                    count--;
                }
            }
            index++;
            if(index == cycle) {
                index = 0;
            }
        }
    }

}
