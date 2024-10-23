package boluo.videoclips;

import boluo.common.FileTool;
import boluo.repositories.URLRepository;
import boluo.videoclips.commands.Op;
import boluo.videoclips.commands.OpContext;
import boluo.videoclips.commands.RecordOp;
import boluo.videoclips.commands.VideoClipsCommand;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.*;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private MixVideoService mixVideoService;

    public void videoClip(@Valid VideoClipsCommand command) {
        //剪辑
        URL url = doVideoClip(command.getUrl(), command.getOps());
        //合并
        if(command.getMix() != null && CollectionUtil.isNotEmpty(command.getMix().getUrls())) {
            List<URL> mixUrls;
            if(command.getMix().isJoinVC()) {
                mixUrls = command.getMix().getUrls().stream().map(it -> doVideoClip(it, command.getOps())).toList();
            }else {
                mixUrls = command.getMix().getUrls().stream().map(it -> urlRepository.toURL(it)).toList();
            }
            url = mixVideoService.mixVideo(url, mixUrls, command.getMix().getJoinWay());
        }
        //转码
        transcode(url, command.getTargetUrls());
    }

    /**
     * 视频剪辑
     * 返回剪辑后的视频，没有剪辑操作返回源视频地址
     * */
    protected URL doVideoClip(final String originUrl, final List<Op> opList) {
        if(CollectionUtil.isEmpty(opList)) {
            //没有剪辑操作，源视频地址返回
            return urlRepository.toURL(originUrl);
        }
        FrameGrabber grabber = null;
        FrameRecorder recorder = null;
        List<Op> ops = new ArrayList<>(opList.size() + 1);
        ops.addAll(opList);
        ops.sort(Comparator.comparing(Op::order));
        try {
            ops.forEach(Op::start);
            URL url = urlRepository.toURL(originUrl);
            grabber = ffmpegFactory.buildFrameGrabber(url);
            grabber.start();
            List<FrameGrabStarter> frameGrabStarters = ops.stream().filter(it -> it instanceof FrameGrabStarter).map(it -> (FrameGrabStarter)it).toList();
            for(FrameGrabStarter fgs : frameGrabStarters) {
                fgs.afterStart(grabber);
            }
            String suffix = FileUtil.getSuffix(url.getPath());
            URL localURL = URLUtil.url(FileTool.buildTmpFile(suffix));
            recorder = ffmpegFactory.buildFrameRecorder(localURL, grabber);
            List<FrameRecordStarter> frameRecordStarters = ops.stream().filter(it -> it instanceof FrameRecordStarter).map(it -> (FrameRecordStarter) it).toList();
            for(FrameRecordStarter frs : frameRecordStarters) {
                frs.beforeStart(recorder);
            }
            recorder.start();
            for(FrameRecordStarter frs : frameRecordStarters) {
                frs.afterStart(recorder);
            }
            RecordOp op = new RecordOp(recorder);
            op.start();
            ops.add(op);
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
            log.info("video clips (url {}) record completed", originUrl);
            ops.forEach(Op::complete);
            return localURL;
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            for(Op op : ops) {
                try{
                    op.close();
                }catch (Throwable e) {
                    log.warn("op close fail {}", op.getClass(), e);
                }
            }
            if(recorder != null) {
                try {
                    recorder.close();
                } catch (FrameRecorder.Exception e) {
                    log.warn("FFmpegFrameRecorder close fail", e);
                }
            }
            if(grabber != null) {
                try {
                    grabber.close();
                } catch (FrameGrabber.Exception e) {
                    log.warn("FFmpegFrameGrabber close fail", e);
                }
            }
        }
    }

    protected void transcode(URL url, List<String> targets) {
        try{
            String urlSuffix = FileUtil.getSuffix(url.getPath());
            List<URL> targetUrls = targets.stream().map(it -> urlRepository.toURL(it)).toList();
            for(URL targetUrl : targetUrls) {
                if(urlSuffix.equalsIgnoreCase(FileUtil.getSuffix(targetUrl.getPath()))) { //相同后缀文件处理 file:copy,remote:upload
                    try(InputStream input = new FileInputStream(url.getPath())) {
                        urlRepository.upload(targetUrl, input);
                    }
                }else {//不同文件后缀处理
                    doTranscode(url, targetUrl);
                }
            }
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            FileTool.deleteLocalTmpFile(url);
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
            long start = System.currentTimeMillis();
            recorder.start();
            log.info("video transcode (url {}) (targetUrl {}) start completed", url, targetUrl);
            Frame frame;
            while((frame = grabber.grab()) != null) {
                recorder.record(frame);
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

    public long getLengthTime(String url) {
        try(FFmpegFrameGrabber grabber = (FFmpegFrameGrabber) ffmpegFactory.buildFrameGrabber(urlRepository.toURL(url))) {
            grabber.start();
            return grabber.getLengthInTime();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
