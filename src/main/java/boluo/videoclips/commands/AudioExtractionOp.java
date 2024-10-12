package boluo.videoclips.commands;

import boluo.common.SpringContext;
import boluo.repositories.URLRepository;
import boluo.videoclips.FFmpegFactory;
import boluo.videoclips.FrameGrabStarter;
import boluo.videoclips.LocalFFmpegFrameRecorder;
import boluo.videoclips.OpChain;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.util.List;

@Slf4j
public class AudioExtractionOp extends Op implements FrameGrabStarter {

    @Setter @Getter
    @NotBlank(message = "targetUrl is empty")
    private String targetUrl;
    @Setter @Getter
    private String audioCodecName;
    @Setter @Getter
    private String format;
    private FrameRecorder recorder;

    @Override
    public void start() {
        URLRepository repository = SpringContext.getBean(URLRepository.class);
        String suffix = FileUtil.getSuffix(repository.toURL(targetUrl).getPath()).toLowerCase();
        if(!List.of("mp3", "wav", "aac", "ogg").contains(suffix)) {
            throw new IllegalArgumentException(suffix + " is not supported");
        }
    }

    @Override
    public void afterStart(FrameGrabber grabber) {
        URLRepository repository = SpringContext.getBean(URLRepository.class);
        FFmpegFactory factory = SpringContext.getBean(FFmpegFactory.class);
        recorder = factory.buildAudioRecorder(repository.toURL(targetUrl), grabber);
        if(StrUtil.isNotBlank(audioCodecName)) {
            recorder.setAudioCodecName(audioCodecName);
        }
        if(StrUtil.isNotBlank(format)) {
            recorder.setFormat(format);
        }
        try {
            recorder.start();
        } catch (FrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        if(frame.samples != null) {
            try {
                recorder.record(frame);
            } catch (FrameRecorder.Exception e) {
                throw new RuntimeException(e);
            }
        }
        chain.doFilter(context, frame);
    }

    @Override
    public void complete() {
        if(recorder instanceof LocalFFmpegFrameRecorder localRecorder) {
            localRecorder.setComplete(true);
        }
    }

    @Override
    public void close() {
        if(recorder != null) {
            try {
                recorder.close();
            } catch (FrameRecorder.Exception e) {
                log.warn("FrameRecorder close fail url {}", targetUrl, e);
            }
        }
    }

}
