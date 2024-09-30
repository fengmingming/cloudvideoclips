package boluo.repositories;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

@Component
@Primary
public class URLRepositoryDelegate implements URLRepository {

    @Resource
    private List<URLRepository> repositories;

    @Override
    public boolean support(URL url) {
        return repositories.stream().filter(it -> it.support(url)).findFirst().isPresent();
    }

    @Override
    public boolean support(String url) {
        return repositories.stream().filter(it -> it.support(url)).findFirst().isPresent();
    }

    @Override
    public URL toURL(String url) {
        Optional<URLRepository> repositoryOpt = repositories.stream().filter(it -> it.support(url)).findFirst();
        if(repositoryOpt.isPresent()) {
            return repositoryOpt.get().toURL(url);
        }
        throw new RuntimeException(url.toString() + " is not supported");
    }

    @Override
    public boolean exist(URL url) {
        Optional<URLRepository> repositoryOpt = repositories.stream().filter(it -> it.support(url)).findFirst();
        if(repositoryOpt.isPresent()) {
            return repositoryOpt.get().exist(url);
        }
        throw new RuntimeException(url.toString() + " is not supported");
    }

    @Override
    public InputStream inputStream(URL url) {
        Optional<URLRepository> repositoryOpt = repositories.stream().filter(it -> it.support(url)).findFirst();
        if(repositoryOpt.isPresent()) {
            return repositoryOpt.get().inputStream(url);
        }
        throw new RuntimeException(url.toString() + " is not supported");
    }

    @Override
    public void upload(URL url, InputStream input) {
        Optional<URLRepository> repositoryOpt = repositories.stream().filter(it -> it.support(url)).findFirst();
        if(repositoryOpt.isPresent()) {
            repositoryOpt.get().upload(url, input);
        }else {
            throw new RuntimeException(url.toString() + " is not supported");
        }
    }

}
