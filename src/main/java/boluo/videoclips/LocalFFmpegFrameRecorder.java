package boluo.videoclips;

import boluo.repositories.LocalFileInputStream;
import boluo.common.SpringContext;
import boluo.repositories.URLRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;


@Slf4j
public class LocalFFmpegFrameRecorder extends FFmpegFrameRecorder {

    @Setter
    private boolean complete;
    @Setter
    private URL targetUrl;
    @Setter
    private File localFile;

    public LocalFFmpegFrameRecorder(URL url, int audioChannels) {
        super(url, audioChannels);
    }

    public LocalFFmpegFrameRecorder(File file, int audioChannels) {
        super(file, audioChannels);
    }

    public LocalFFmpegFrameRecorder(String filename, int audioChannels) {
        super(filename, audioChannels);
    }

    public LocalFFmpegFrameRecorder(URL url, int imageWidth, int imageHeight) {
        super(url, imageWidth, imageHeight);
    }

    public LocalFFmpegFrameRecorder(File file, int imageWidth, int imageHeight) {
        super(file, imageWidth, imageHeight);
    }

    public LocalFFmpegFrameRecorder(String filename, int imageWidth, int imageHeight) {
        super(filename, imageWidth, imageHeight);
    }

    public LocalFFmpegFrameRecorder(URL url, int imageWidth, int imageHeight, int audioChannels) {
        super(url, imageWidth, imageHeight, audioChannels);
    }

    public LocalFFmpegFrameRecorder(File file, int imageWidth, int imageHeight, int audioChannels) {
        super(file, imageWidth, imageHeight, audioChannels);
    }

    public LocalFFmpegFrameRecorder(String filename, int imageWidth, int imageHeight, int audioChannels) {
        super(filename, imageWidth, imageHeight, audioChannels);
    }

    public LocalFFmpegFrameRecorder(OutputStream outputStream, int audioChannels) {
        super(outputStream, audioChannels);
    }

    public LocalFFmpegFrameRecorder(OutputStream outputStream, int imageWidth, int imageHeight) {
        super(outputStream, imageWidth, imageHeight);
    }

    public LocalFFmpegFrameRecorder(OutputStream outputStream, int imageWidth, int imageHeight, int audioChannels) {
        super(outputStream, imageWidth, imageHeight, audioChannels);
    }

    @Override
    public void close() throws FrameRecorder.Exception {
        FrameRecorder.Exception te = null;
        try{
            super.close();
        }catch (FrameRecorder.Exception e) {
            te = e;
        }
        if(complete) {
            //把local file 上传到targetUrl
            URL targetUrl = this.targetUrl;
            URLRepository repository = SpringContext.getBean(URLRepository.class);
            try(LocalFileInputStream input = new LocalFileInputStream(this.localFile)){
                repository.upload(targetUrl, input);
            }catch (Throwable e) {
                throw new RuntimeException(e);
            }
            //成功上传删除本地临时文件
            this.localFile.delete();
        }
        if(te != null) {
            throw te;
        }
    }

}
