package boluo.videoclips.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class VideoClipsCommand {

    /**
     * 源文件地址
     * */
    @NotBlank(message = "url is blank")
    private String url;
    /**
     * 操作
     * */
    @Valid
    @NotNull
    private List<Op> ops = List.of();
    @Valid
    private VideoMixCommand mix;
    /**
     * 目标文件地址
     * */
    @NotNull
    private List<String> targetUrls;
    private String callbackUrl;

}
