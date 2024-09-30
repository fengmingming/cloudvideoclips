package boluo.videoclips;

import boluo.common.ResVo;
import boluo.videoclips.commands.VideoClipCommand;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class VideoClipRest implements DisposableBean, InitializingBean {

    @Setter
    @Resource
    private VideoClipService videoClipService;
    @Setter
    @Resource
    private RedissonClient redissonClient;
    @Setter
    @Resource
    private VideoClipCallbackService clipCallbackService;

    private ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);

    private List<String> supportedUrlSuffix = List.of("mp4", "MP4");

    private List<String> supportedTargetSuffix = List.of("mp4", "MP4", "m3u8", "M3U8");

    private ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void destroy() throws Exception {
        if(!es.isShutdown()) {
            es.shutdown();
        }
        if(!watchdog.isShutdown()) {
            watchdog.shutdownNow();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        watchdog.scheduleWithFixedDelay(() -> {
            if(es instanceof ThreadPoolExecutor tpe) {
                log.info("video clips executor info corePoolSize={}, taskCount = {}, activeCount = {}", tpe.getCorePoolSize(), tpe.getTaskCount(), tpe.getActiveCount());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }


    @Setter
    @Getter
    public static class DoClipsReq {
        @NotBlank(message = "key is blank")
        private String key;
        @NotNull(message = "commands is null")
        @Size(min = 1, message = "commands size is 0")
        @Valid
        private List<VideoClipCommand> commands;
        private String callbackUrl;
    }

    @PostMapping("/VideoClips")
    public ResVo<?> doClips(@Valid @RequestBody DoClipsReq req) {
        if(req.getCommands().stream().anyMatch(it -> !supportedUrlSuffix.contains(FileUtil.getSuffix(it.getUrl())))) {
            return ResVo.error("只支持mp4格式的文件剪辑");
        }
        if(req.getCommands().stream().flatMap(it -> it.getTargetUrls().stream()).anyMatch(it ->
                !supportedTargetSuffix.contains(FileUtil.getSuffix(it)))) {
            return ResVo.error("只支持mp4或m3u8格式的文件输出");
        }
        if(running(req.getKey())) {
            return ResVo.error("剪辑中...");
        }
        log.info("/VideoClips req = {}", JSONUtil.toJsonStr(req));
        es.submit(() -> {
            RLock lock = redissonClient.getLock(buildRedisKey(req.getKey()));
            if(lock.tryLock()) {
                try{
                    List<Future<Boolean>> fs = req.getCommands().stream().map(command -> {
                        return es.submit(() -> {
                            VideoClipCallbackService.CallbackReq callbackReq = new VideoClipCallbackService.CallbackReq();
                            try{
                                videoClipService.videoClip(command);
                                callbackReq.setStatus(true);
                                return true;
                            }catch (Throwable e) {
                                log.error("do videoClips fail", e);
                                callbackReq.setStatus(false);
                                return false;
                            }finally {
                                if(StrUtil.isNotBlank(command.getCallbackUrl())) {
                                    clipCallbackService.callback(command.getCallbackUrl(), callbackReq);
                                }
                            }
                        });
                    }).collect(Collectors.toList());
                    boolean runState = fs.stream().map(it -> {
                        try{
                            return it.get();
                        }catch (Throwable e) {
                            log.error("future get fail", e);
                        }
                        return true;
                    }).noneMatch(it -> it == false);
                    if(StrUtil.isNotBlank(req.getCallbackUrl())) {
                        VideoClipCallbackService.CallbackReq callbackReq = new VideoClipCallbackService.CallbackReq();
                        callbackReq.setStatus(runState);
                        clipCallbackService.callback(req.getCallbackUrl(), callbackReq);
                    }
                }finally {
                    lock.unlock();
                }
            }
        });
        return ResVo.success();
    }

    @GetMapping(value = "/VideoClips", params = "key")
    public ResVo<?> doClips(@RequestParam("key") String key) {
        boolean running = running(key);
        return ResVo.success(MapUtil.builder().put("running", running).build());
    }

    private boolean running(String key) {
        RLock lock = redissonClient.getLock(buildRedisKey(key));
        if(lock.tryLock()) {//获得锁失败，运行中
            try{
                return false;
            }finally {
                lock.unlock();
            }
        }else {
            return true;
        }
    }

    private String buildRedisKey(String key) {
        return String.format("VideoClips:%s", key);
    }

    @GetMapping(value = "/VideoClips", params = "url")
    public ResVo<?> getLengthTime(@RequestParam("url") String url) {
        if(!supportedUrlSuffix.contains(FileUtil.getSuffix(url))) {
            return ResVo.error("只支持mp4格式的文件");
        }
        long lengthTime = videoClipService.getLengthTime(url);
        return ResVo.success(MapUtil.builder()
                        .put("lengthTime", lengthTime)
                .build());
    }

}
