package boluo.videoclips.commands;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VideoMixCommand {


    private List<String> urls;
    private boolean joinVC;
    //1追加 2混合
    private int joinWay = 1;

}
