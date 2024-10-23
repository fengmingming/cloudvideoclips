package boluo.common;

import boluo.videoclips.VideoClipsConfig;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class FileTool {

    public static void deleteLocalTmpFile(URL url) {
        if(isLocalURL(url)) {
            File file = new File(url.getPath());
            VideoClipsConfig videoClipsConfig = SpringContext.getBean(VideoClipsConfig.class);
            if(FileUtil.isSub(videoClipsConfig.getTmpFileDir(), file)) {
                boolean b = file.delete();
                if(!b) {
                    log.warn("{} delete fail", url);
                }
            }
        }
    }

    public static String buildTmpFile(String suffix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        VideoClipsConfig videoClipsConfig = SpringContext.getBean(VideoClipsConfig.class);
        return String.format("%s/%s/%s.%s", videoClipsConfig.getTmpDir(), LocalDateTime.now().format(formatter),
                IdUtil.fastSimpleUUID(), suffix);
    }

    public static boolean isLocalURL(URL url) {
        return "file".equalsIgnoreCase(url.getProtocol());
    }

}
