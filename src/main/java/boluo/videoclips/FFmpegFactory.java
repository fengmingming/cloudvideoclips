package boluo.videoclips;

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

    public FrameRecorder buildFrameRecorder(URL url, FrameGrabber grabber) {
        FrameRecorder recorder = innerBuildFrameRecorder(url, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.setVideoCodecName(grabber.getVideoCodecName());
        recorder.setVideoMetadata(grabber.getVideoMetadata());
        recorder.setVideoOptions(grabber.getVideoOptions());
        recorder.setVideoSideData(grabber.getVideoSideData());
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setCharset(grabber.getCharset());
        recorder.setAudioBitrate(grabber.getAudioBitrate());
        recorder.setAudioCodec(grabber.getAudioCodec());
        recorder.setAudioCodecName(grabber.getAudioCodecName());
        recorder.setAudioMetadata(grabber.getAudioMetadata());
        recorder.setAudioOptions(grabber.getAudioOptions());
        recorder.setAudioSideData(grabber.getAudioSideData());
        recorder.setSampleRate(grabber.getSampleRate());
        if(url.getPath().endsWith("m3u8") || url.getPath().endsWith("M3U8")) {
            recorder.setGopSize((int) grabber.getFrameRate());
            recorder.setFormat("hls");
            recorder.setOption("hls_time", "10");
            recorder.setOption("hls_list_size", "0");
            recorder.setOption("hls_segment_type", "mpegts");
            recorder.setOption("hls_flags", "independent_segments");
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
