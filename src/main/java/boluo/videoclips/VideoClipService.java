package boluo.videoclips;

import boluo.repositories.URLRepository;
import boluo.videoclips.commands.Op;
import boluo.videoclips.commands.OpContext;
import boluo.videoclips.commands.VideoClipCommand;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.*;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
@Setter
public class VideoClipService {

    @Resource
    private FFmpegFactory ffmpegFactory;
    @Resource
    private URLRepository urlRepository;
    @Resource
    private VideoClipConfig videoClipConfig;

    @Retryable(maxAttempts = 3)
    public void videoClip(@Valid VideoClipCommand command) {
        FrameGrabber grabber = null;
        List<FrameRecorder> recorders = null;
        try{
            URL url = urlRepository.toURL(command.getUrl());
            List<URL> targetUrls = command.getTargetUrl().stream().map(urlRepository::toURL).collect(Collectors.toList());
            grabber = ffmpegFactory.buildFrameGrabber(url);
            grabber.start();
            FrameGrabber grabberFinal = grabber;
            recorders = targetUrls.stream().map(it -> ffmpegFactory.buildFrameRecorder(it, grabberFinal)).collect(Collectors.toList());
            for(FrameRecorder recorder : recorders) {
                recorder.start();
            }
            command.getOps().forEach(op -> op.start());
            log.info("video clips (url {}) start completed", command.getUrl());
            final long startTime = System.currentTimeMillis();
            long preTime = startTime;
            Frame frame = null;
            OpContext opContext = new OpContext();
            while((frame = grabber.grab()) != null) {
                for(Op op : command.getOps()) {
                    frame = op.doFilter(opContext, frame);
                    if(frame == null) {
                        break;
                    }
                }
                if(frame == null) {
                    continue;
                }
                for(FrameRecorder recorder : recorders) {
                    recorder.record(frame);
                }
                if(System.currentTimeMillis() - preTime > 10 * 1000) {
                    preTime = System.currentTimeMillis();
                    log.info("do videoClip url = {}, run time = {} frame.timestamp = {}",
                            command.getUrl(), DateUtil.formatBetween(preTime - startTime), DateUtil.formatBetween(frame.timestamp/1000));
                }
            }
            command.getOps().forEach(op -> op.close());
            //设置是否处理完成
            for(FrameRecorder recorder : recorders) {
                if(recorder instanceof LocalFFmpegFrameRecorder localRecorder) {
                    localRecorder.setComplete(true);
                }
            }
            log.info("video clips (url {}) record completed", command.getUrl());
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            if(grabber != null) {
                try {
                    grabber.close();
                } catch (FrameGrabber.Exception e) {
                    log.warn("FFmpegFrameGrabber close fail", e);
                }
            }
            if(recorders != null) {
                recorders.forEach(it -> {
                    try {
                        it.close();
                    } catch (FrameRecorder.Exception e) {
                        log.warn("FFmpegFrameRecorder close fail", e);
                    }
                });
            }
            log.info("video clips (url {}) output completed", command.getUrl());
        }
    }

    //@Retryable(maxAttempts = 3)
    public void videoClipV2(@Valid VideoClipCommand command) {
        URL url = doVideoClip(command);
        transcode(url, command.getTargetUrl());
    }

    /**
     * 视频剪辑
     * 返回剪辑后的视频，没有剪辑操作返回源视频地址
     * */
    protected URL doVideoClip(VideoClipCommand command) {
        if(CollectionUtil.isEmpty(command.getOps())) {
            //没有剪辑操作，源视频地址返回
            return urlRepository.toURL(command.getUrl());
        }
        FrameGrabber grabber = null;
        FrameRecorder recorder = null;
        try {
            URL url = urlRepository.toURL(command.getUrl());
            grabber = ffmpegFactory.buildFrameGrabber(url);
            grabber.start();
            List<URL> targetUrls = command.getTargetUrl().stream().map(it -> urlRepository.toURL(it)).collect(Collectors.toList());
            //如果没有本地剪辑文件，创建本地剪辑文件
            String suffix = FileUtil.getSuffix(url.getPath());
            Optional<URL> localURLOpt = targetUrls.stream().filter(it -> "file".equalsIgnoreCase(it.getProtocol())
                    && suffix.equalsIgnoreCase(FileUtil.getSuffix(it.getPath()))).findFirst();
            URL localURL;
            if(localURLOpt.isPresent()) {
                localURL = localURLOpt.get();
            }else {
                localURL = URLUtil.url(buildTmpFile(suffix));
                log.info("create tmp file url = {}", localURL.toString());
            }
            recorder = ffmpegFactory.buildFrameRecorder(localURL, grabber);
            recorder.start();
            command.getOps().forEach(it -> it.start());
            log.info("video clips (url {}) start completed", command.getUrl());
            final long startTime = System.currentTimeMillis();
            long preTime = startTime;
            OpContext opContext = new OpContext();
            Frame frame;
            while((frame = grabber.grab()) != null) {
                for(Op op : command.getOps()) {
                    frame = op.doFilter(opContext, frame);
                    if(frame == null) {
                        break;
                    }
                }
                if(frame == null) {
                    continue;
                }
                recorder.record(frame);
                if(System.currentTimeMillis() - preTime > 10 * 1000) {
                    preTime = System.currentTimeMillis();
                    log.info("do videoClip url = {}, run time = {} frame.timestamp = {}",
                            command.getUrl(), DateUtil.formatBetween(preTime - startTime), DateUtil.formatBetween(frame.timestamp/1000));
                }
            }
            command.getOps().forEach(it -> it.close());
            log.info("video clips (url {}) record completed", command.getUrl());
            return localURL;
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            if(grabber != null) {
                try {
                    grabber.close();
                } catch (FrameGrabber.Exception e) {
                    log.warn("FFmpegFrameGrabber close fail", e);
                }
            }
            if(recorder != null) {
                try {
                    recorder.close();
                } catch (FrameRecorder.Exception e) {
                    log.warn("FFmpegFrameRecorder close fail", e);
                }
            }
        }
    }

    protected void transcode(URL url, List<String> targets) {
        try{
            List<URL> targetUrls = targets.stream().map(it -> urlRepository.toURL(it)).collect(Collectors.toList());
            if("file".equalsIgnoreCase(url.getProtocol())) {
                //去掉本地在剪辑的时候已经生成的文件
                File file = new File(url.getPath());
                if(!FileUtil.isSub(videoClipConfig.getTmpFileDir(), file)) {
                    targetUrls = targetUrls.stream().filter(it -> !(url.getProtocol().equalsIgnoreCase(it.getProtocol())
                        && url.getPath().equals(it.getPath()))).collect(Collectors.toList());
                }
            }
            for(URL targetUrl : targetUrls) {
                doTranscode(url, targetUrl);
            }
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            if("file".equalsIgnoreCase(url.getProtocol())) {
                File file = new File(url.getPath());
                if(FileUtil.isSub(videoClipConfig.getTmpFileDir(), file)) {
                    file.delete();
                }
            }
        }
    }

    protected void doTranscode(URL url, URL targetUrl) {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        try{
            grabber = (FFmpegFrameGrabber) ffmpegFactory.buildFrameGrabber(url);
            grabber.start();
            FrameGrabber grabberFinal = grabber;
            recorder = (FFmpegFrameRecorder) ffmpegFactory.buildFrameRecorder(targetUrl, grabberFinal);
            recorder.start(grabber.getFormatContext());
            log.info("video transcode (url {}) (targetUrl {}) start completed", url, targetUrl);
            long start = System.currentTimeMillis();
            AVPacket packet;
            while((packet = grabber.grabPacket()) != null) {
                recorder.recordPacket(packet);
            }
            if(recorder instanceof LocalFFmpegFrameRecorder localRecorder) {
                localRecorder.setComplete(true);
            }
            log.info("video transcode (url {}) (targetUrl {}) record completed run time = {}", url, targetUrl, System.currentTimeMillis() - start);
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            if(grabber != null) {
                try {
                    grabber.close();
                } catch (FrameGrabber.Exception e) {
                    log.warn("FFmpegFrameGrabber close fail", e);
                }
            }
            if(recorder != null) {
                try {
                    recorder.close();
                } catch (FrameRecorder.Exception e) {
                    log.warn("FFmpegFrameRecorder close fail", e);
                }
            }
        }
    }

    private String buildTmpFile(String suffix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        return String.format("%s/%s/%s.%s", videoClipConfig.getTmpDir(), LocalDateTime.now().format(formatter),
                IdUtil.fastSimpleUUID(), suffix);
    }

    public long getLengthTime(String url) {
        try(FFmpegFrameGrabber grabber = (FFmpegFrameGrabber) ffmpegFactory.buildFrameGrabber(urlRepository.toURL(url))) {
            grabber.start();
            return grabber.getLengthInTime();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
