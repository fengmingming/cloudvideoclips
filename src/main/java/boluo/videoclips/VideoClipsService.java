package boluo.videoclips;

import boluo.repositories.URLRepository;
import boluo.videoclips.commands.*;
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
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
@Setter
public class VideoClipsService {

    @Resource
    private FFmpegFactory ffmpegFactory;
    @Resource
    private URLRepository urlRepository;
    @Resource
    private VideoClipsConfig videoClipConfig;

    public void videoClip(@Valid VideoClipsCommand command) {
        //剪辑
        URL url = doVideoClip(command.getUrl(), command.getOps(), command.getTargetUrls());
        //合并
        if(command.getMix() != null && CollectionUtil.isNotEmpty(command.getMix().getUrls())) {
            List<URL> mixUrls = null;
            if(command.getMix().isJoinVC()) {
                mixUrls = command.getMix().getUrls().stream().map(it -> doVideoClip(it, command.getOps(), List.of())).toList();
            }else {
                mixUrls = command.getMix().getUrls().stream().map(it -> urlRepository.toURL(it)).toList();
            }
            url = mixVideo(url, mixUrls, command.getMix().getJoinWay());
        }
        //转码
        transcode(url, command.getTargetUrls());
    }

    /**
     * 视频剪辑
     * 返回剪辑后的视频，没有剪辑操作返回源视频地址
     * */
    protected URL doVideoClip(final String originUrl, final List<Op> opList, final List<String> targets) {
        if(CollectionUtil.isEmpty(opList)) {
            //没有剪辑操作，源视频地址返回
            return urlRepository.toURL(originUrl);
        }
        FrameGrabber grabber = null;
        FrameRecorder recorder = null;
        try {
            URL url = urlRepository.toURL(originUrl);
            grabber = ffmpegFactory.buildFrameGrabber(url);
            grabber.start();
            List<URL> targetUrls = targets.stream().map(it -> urlRepository.toURL(it)).toList();
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
            List<Op> ops = new ArrayList<>(opList.size() + 1);
            ops.addAll(opList);
            ops.sort(Comparator.comparing(Op::order));
            ops.add(new RecordOp(recorder));
            ops.forEach(Op::start);
            log.info("video clips (url {}) start completed", originUrl);
            final long startTime = System.currentTimeMillis();
            long preTime = startTime;
            OpChain opChain = new OpChain(ops);
            OpContext opContext = new OpContext();
            Frame frame;
            while((frame = grabber.grab()) != null) {
                if(System.currentTimeMillis() - preTime > 10 * 1000) {
                    preTime = System.currentTimeMillis();
                    log.info("do videoClip url = {}, run time = {} frame.timestamp = {}", originUrl, DateUtil.formatBetween(preTime - startTime), DateUtil.formatBetween(frame.timestamp/1000));
                }
                opChain.restart();
                opChain.doFilter(opContext, frame);
            }
            ops.forEach(Op::close);
            log.info("video clips (url {}) record completed", originUrl);
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
                        && url.getPath().equals(it.getPath()))).toList();
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

    protected URL mixVideo(URL url, List<URL> urls, int joinWay) {
        return switch (joinWay) {
            case 1 -> concatVideo(url, urls);
            case 2 -> overlying(url, urls);
            default -> throw new IllegalArgumentException("joinWay=" + joinWay + " is not supported");
        };
    }

    protected URL concatVideo(URL url, List<URL> urls) {
        return null;
    }

    protected URL overlying(URL url, List<URL> urls) {
        return null;
    }

}
