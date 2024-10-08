package boluo.videoclips.commands;

import boluo.videoclips.OpChain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放速度操作
 * */
public class SpeedOp extends Op{

    @NotBlank
    @Pattern(regexp = "0\\.25|0\\.5|0\\.75|1|1\\.25|1\\.5|1\\.75|2|3", message = "value can only be 0.25,0.5,0.75,1,1.25,1.5,1.75,2,3")
    private @Setter @Getter String value;
    private @Setter @Getter long startTime;
    private @Setter @Getter long endTime = Long.MAX_VALUE;
    private int times;
    private int cycle;
    private List<Integer> points;
    private int index;

    @Override
    public void start() {
        points = null;
        times = 0;
        cycle = 0;
        index = 0;
        switch (value) {
            case "0.25":
                times = 4;
                cycle = 1;
                break;
            case "0.5":
                times = 2;
                cycle = 1;
                break;
            case "0.75":
                times = 4;
                cycle = 3;
                break;
            case "1":
                cycle = 1;
                points = List.of(0);
                break;
            case "1.25":
                cycle = 5;
                points = List.of(0, 1, 2, 3);
                break;
            case "1.5":
                cycle = 3;
                points = List.of(0, 1);
                break;
            case "1.75":
                cycle = 7;
                points = List.of(0, 1, 2, 3);
                break;
            case "2":
                cycle = 2;
                points = List.of(0);
                break;
            case "3":
                cycle = 3;
                points = List.of(0);
                break;
            default: throw new IllegalArgumentException("value can only be 0.25,0.5,0.75,1,1.25,1.5,1.75,2,3");
        }
    }

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        if(frame.timestamp < startTime || frame.timestamp > endTime) {
            chain.doFilter(context, frame);
            return;
        }
        if(points == null) {//慢放
            index++;
            if(index == cycle) {
                List<Frame> frames = new ArrayList<>(times);
                frames.add(frame);
                for(int i = times - cycle; i > 0; i--) {
                    frames.add(frame.clone());
                }
                index = 0;
                chain.doFilter(context, frames);
            }
        }else {//加速
            if(index < cycle) {
                if(points.contains(index)) {
                    chain.doFilter(context, frame);
                }
            }
            index++;
            if(index == cycle) {
                index = 0;
            }
        }
    }

}
