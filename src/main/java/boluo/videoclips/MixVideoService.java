package boluo.videoclips;

import boluo.common.FileTool;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.annotation.Resource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Setter
@Component
@Slf4j
public class MixVideoService {

    @Resource
    private FFmpegFactory ffmpegFactory;

    public URL mixVideo(URL url, List<URL> urls, int joinWay) {
        return switch (joinWay) {
            case 1 -> concatVideo(url, urls);
            case 2 -> overlying(url, urls);
            default -> throw new IllegalArgumentException("joinWay=" + joinWay + " is not supported");
        };
    }

    protected URL concatVideo(URL url, List<URL> urls) {
        FrameGrabber grabber = null;
        List<FrameGrabber> concatGrabbers = null;
        FrameRecorder recorder = null;
        try{
            grabber = ffmpegFactory.buildFrameGrabber(url);
            grabber.start();
            concatGrabbers = urls.stream().map(ffmpegFactory::buildFrameGrabber).toList();
            for(FrameGrabber other : concatGrabbers) {
                other.start();
            }
            URL localURL = URLUtil.url(FileTool.buildTmpFile(FileUtil.getSuffix(url.getPath())));
            recorder = ffmpegFactory.buildFrameRecorder(localURL, grabber);
            recorder.start();
            copyFrame(grabber, recorder);
            for(FrameGrabber other : concatGrabbers) {
                copyFrame(other, recorder);
            }
            return localURL;
        }catch (Throwable e) {
            log.error("concat video exception", e);
            throw new RuntimeException(e);
        }finally {
            if(grabber != null) {
                try {
                    grabber.close();
                } catch (FrameGrabber.Exception e) {
                    log.warn("concat video grabber close fail url {}", url, e);
                }
            }
            if(concatGrabbers != null) {
                concatGrabbers.forEach(it -> {
                    try {
                        it.close();
                    } catch (FrameGrabber.Exception e) {
                        log.warn("concat video mix grabber close fail", e);
                    }
                });
            }
            if(recorder != null) {
                try {
                    recorder.close();
                } catch (FrameRecorder.Exception e) {
                    log.warn("concat video recorder close fail", e);
                }
            }
            FileTool.deleteLocalTmpFile(url);
        }
    }

    /**
     * 将多个视频合并成一个视频
     * 音频合并，多个画面重新布局
     * */
    protected URL overlying(URL url, List<URL> urls) {
        List<URL> urlList = new ArrayList<>();
        urlList.add(url);
        urlList.addAll(urls);
        List<FrameGrabber> grabbers = null;
        FrameRecorder recorder = null;
        try{
            grabbers = urlList.stream().map(it -> ffmpegFactory.buildFrameGrabber(it)).toList();
            for(FrameGrabber g : grabbers) {
                g.start();
            }
            URL localUrl = URLUtil.url(FileTool.buildTmpFile(FileUtil.getSuffix(url.getPath())));
            recorder = ffmpegFactory.buildFrameRecorder(url, grabbers.get(0));

            return localUrl;
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            if(grabbers != null) {
                for(FrameGrabber g : grabbers) {
                    try {
                        g.close();
                    } catch (FrameGrabber.Exception e) {
                        log.warn("overlying video grabber close fail", e);
                    }
                }
            }
            if(recorder != null) {
                try {
                    recorder.close();
                } catch (FrameRecorder.Exception e) {
                    log.warn("overlying video recorder close fail", e);
                }
            }
            FileTool.deleteLocalTmpFile(url);
        }
    }

    /**
     *
     * */
    protected void copyFrame(FrameGrabber grabber, FrameRecorder recorder) throws Exception {
        Frame frame;
        while((frame = grabber.grab()) != null) {
            recorder.record(frame);
        }
    }

}
