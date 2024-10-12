package boluo.repositories;

import boluo.videoclips.VideoClipsConfig;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

@Component
@Setter
public class LocalURLRepository implements URLRepository {

   @Resource
   private VideoClipsConfig vcConfig;

    @Override
    public boolean support(URL url) {
        return "file".equalsIgnoreCase(url.getProtocol());
    }

    @Override
    public boolean support(String url) {
        if(StrUtil.isBlank(url)) return false;
        return url.startsWith("/") || url.startsWith("file:/");
    }

    @Override
    public boolean exist(URL url) {
        File file = new File(url.getPath());
        return file.exists();
    }

    @Override
    public InputStream inputStream(URL url) {
        File file = new File(url.getPath());
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upload(URL url, InputStream input) {
        File file = new File(url.getPath());
        if(file.exists()) {
            file.delete();
        }
        try (FileOutputStream output = new FileOutputStream(file)){
            IOUtils.copyLarge(input, output);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL toURL(String url) {
        if(url.startsWith("file:")) {
            url = url.substring(5);
        }
        try {
            return new URL("file", null, -1, vcConfig.getLocalRoot() + url, new NullURLStreamHandler());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
