package boluo.videoclips;

import boluo.common.RecorderConfig;
import boluo.common.URLTool;
import boluo.repositories.URLRepository;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.Map;

@Component
@Setter
public class FFmpegFactory {

    @Resource
    private VideoClipsConfig vcConfig;
    @Resource
    private URLRepository urlRepository;

    public FrameGrabber buildFrameGrabber(URL url) {
        FFmpegFrameGrabber grabber;
        if("file".equalsIgnoreCase(url.getProtocol())) {
            grabber = new FFmpegFrameGrabber(new File(url.getPath()));
        }else if("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol())){
            grabber = new FFmpegFrameGrabber(url);
        }else {
            grabber = new FFmpegFrameGrabber(urlRepository.inputStream(url));
        }
        return grabber;
    }

    public FrameRecorder buildAudioRecorder(URL url, FrameGrabber grabber) {
        RecorderConfig config = URLTool.buildQueryObj(url.getQuery());
        FrameRecorder recorder = innerBuildFrameRecorder(url, 0, 0, grabber.getAudioChannels());
        recorder.setAudioBitrate(config.getInt("audioBitrate", grabber.getAudioBitrate()));
        recorder.setAudioMetadata(config.getMap("audioMetadata", grabber.getAudioMetadata()));
        recorder.setAudioOptions(config.getMap("audioOptions", grabber.getAudioOptions()));
        recorder.setAudioSideData(grabber.getAudioSideData());
        recorder.setSampleRate(config.getInt("sampleRate", grabber.getSampleRate()));
        Map<String, String> options = config.getMap("options", grabber.getOptions());
        recorder.setOptions(options);
        if(config.containsKey("format")) {
            recorder.setFormat(config.getStr("format"));
        }
        if(config.containsKey("audioCodecName")) {
            recorder.setAudioCodecName(config.getStr("audioCodecName"));
        }
        return recorder;
    }

    public FrameRecorder buildFrameRecorder(URL url, FrameGrabber grabber) {
        RecorderConfig config = URLTool.buildQueryObj(url.getQuery());
        FrameRecorder recorder = innerBuildFrameRecorder(url, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setVideoBitrate(config.getInt("videoBitrate", grabber.getVideoBitrate()));
        recorder.setVideoCodec(config.getInt("videoCodec", grabber.getVideoCodec()));
        recorder.setVideoCodecName(config.getStr("videoCodecName", grabber.getVideoCodecName()));
        recorder.setVideoMetadata(config.getMap("videoMetadata", grabber.getVideoMetadata()));
        recorder.setVideoOptions(config.getMap("videoOptions", grabber.getVideoOptions()));
        recorder.setVideoSideData(grabber.getVideoSideData());
        recorder.setFrameRate(config.getDouble("frameRate", grabber.getFrameRate()));
        recorder.setCharset(config.getCharset("charset", grabber.getCharset()));
        recorder.setAudioBitrate(config.getInt("audioBitrate", grabber.getAudioBitrate()));
        recorder.setAudioCodec(config.getInt("audioCodec", grabber.getAudioCodec()));
        recorder.setAudioCodecName(config.getStr("audioCodecName", grabber.getAudioCodecName()));
        recorder.setAudioMetadata(config.getMap("audioMetadata", grabber.getAudioMetadata()));
        recorder.setAudioOptions(config.getMap("audioOptions", grabber.getAudioOptions()));
        recorder.setAudioSideData(grabber.getAudioSideData());
        recorder.setSampleRate(config.getInt("sampleRate", grabber.getSampleRate()));
        Map<String, String> options = config.getMap("options", grabber.getOptions());
        recorder.setOptions(options);
        if(config.containsKey("format")) {
            recorder.setFormat(config.getStr("format"));
        }
        if(url.getPath().endsWith("m3u8") || url.getPath().endsWith("M3U8")) {
            recorder.setGopSize(config.getInt("gopSize", (int) grabber.getFrameRate()));
            recorder.setFormat("hls");
            if(!options.containsKey("hls_time")) {
                recorder.setOption("hls_time", "10");
            }
            if(!options.containsKey("hls_list_size")) {
                recorder.setOption("hls_list_size", "0");
            }
            if(!options.containsKey("hls_segment_type")) {
                recorder.setOption("hls_segment_type", "mpegts");
            }
            if(!options.containsKey("hls_flags")) {
                recorder.setOption("hls_flags", "independent_segments");
            }
        }
        return recorder;
    }

    protected FrameRecorder innerBuildFrameRecorder(URL targetUrl, int imageWidth, int imageHeight, int audioChannels) {
        if("file".equalsIgnoreCase(targetUrl.getProtocol())) {
            File file = new File(targetUrl.getPath());
            if(!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            return new FFmpegFrameRecorder(file, imageWidth, imageHeight, audioChannels);
        }else {
            //用于ftp和oss上传
            File localFile = createTmpFile(targetUrl);
            LocalFFmpegFrameRecorder recorder = new LocalFFmpegFrameRecorder(localFile, imageWidth, imageHeight, audioChannels);
            recorder.setLocalFile(localFile);
            recorder.setTargetUrl(targetUrl);
            return recorder;
        }
    }

    protected File createTmpFile(URL targetUrl) {
        File file = new File(vcConfig.getTmpFileDir(), targetUrl.getPath());
        if(!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

}
