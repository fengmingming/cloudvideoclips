package boluo.videoclips;

import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class VideoClipCallbackService {

    @Setter
    @Getter
    public static class CallbackReq {
        private boolean status;
    }

    private RestTemplate restTemplate = new RestTemplate();

    @Retryable(maxAttempts = 3)
    public void callback(String url, CallbackReq req) {
        log.info("video clips callback url {} req {}", url, JSONUtil.toJsonStr(req));
        restTemplate.put(url, req);
    }

}
