package boluo.repositories;

import boluo.videoclips.VideoClipConfig;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;

@Component
@Setter
public class LocalURLRepository implements URLRepository {

   @Resource
   private VideoClipConfig vcConfig;

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
        return URLUtil.url(vcConfig.getLocalRoot() + url);
    }

}