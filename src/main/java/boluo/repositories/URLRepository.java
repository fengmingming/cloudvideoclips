package boluo.repositories;

import java.io.InputStream;
import java.net.URL;

public interface URLRepository {

    public boolean support(URL url);

    public boolean support(String url);

    public URL toURL(String url);

    public boolean exist(URL url);

    public InputStream inputStream(URL url);

    public void upload(URL url, InputStream input);

}
