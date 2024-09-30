package boluo.repositories;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Setter
@Component
public class HttpURLRepository implements URLRepository {

    @Override
    public boolean support(URL url) {
        return "http".equalsIgnoreCase(url.getProtocol());
    }

    @Override
    public boolean support(String url) {
        if(StrUtil.isBlank(url)) return false;
        return url.startsWith("https://") || url.startsWith("http://");
    }

    @Override
    public URL toURL(String url) {
        return URLUtil.url(url);
    }

    @Override
    public boolean exist(URL url) {
        try {
            InputStream stream = url.openStream();
            stream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public InputStream inputStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upload(URL url, InputStream input) {
        throw new IllegalCallerException();
    }

}
