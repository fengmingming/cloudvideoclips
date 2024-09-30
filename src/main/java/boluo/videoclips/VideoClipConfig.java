package boluo.videoclips;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "vc")
@Setter
@Getter
public class VideoClipConfig implements InitializingBean {

    /**
     * 临时文件目录
     * */
    private String tmpDir = "/tmp";
    /**
     * 本地文件存储目录
     * */
    private String localRoot = "";

    @Override
    public void afterPropertiesSet() throws Exception {
        if(this.tmpDir.endsWith("/")) {
            this.tmpDir = this.tmpDir.substring(0, this.tmpDir.length() - 1);
        }
        this.tmpDir = this.tmpDir.replaceAll("\\\\", "/");
        if(StrUtil.isNotBlank(this.localRoot)) {
            this.localRoot = this.localRoot.replaceAll("\\\\", "/");
        }
    }

    public File getTmpFileDir() {
        return new File(this.tmpDir);
    }

}
