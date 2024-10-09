package boluo.videoclips.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VideoMixCommand {

    @NotNull
    @Size(min = 1, message = "mix urls is empty")
    private List<String> urls;
    private boolean joinVC;
    //1追加 2混合
    @Pattern(regexp = "1|2", message = "joinWay can only be 1 or 2")
    private int joinWay = 1;

}
