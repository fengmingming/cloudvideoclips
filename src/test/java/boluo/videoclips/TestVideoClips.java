package boluo.videoclips;

import boluo.Bootstrap;
import boluo.videoclips.commands.*;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Bootstrap.class, properties = {
        "vc.tmpDir=D:/boluo/temp",
        "vc.localRoot=D:/boluo/vc"
})
public class TestVideoClips {

    @Resource
    private VideoClipsService clipService;

    @Test
    public void testTrim() {
        VideoClipsCommand command = new VideoClipsCommand();
        command.setUrl("https://www.bilibili.com/video/BV1qt421c7HP?t=25.4");
        command.setTargetUrls(List.of("/example.mp4"));
        clipService.videoClip(command);
    }

    @Test
    public void testWatermark() {
        VideoClipsCommand command = new VideoClipsCommand();
        WatermarkOp op = new WatermarkOp();
        op.setImageUrl("https://docbook.com.cn/_nuxt/img/docbook-header-logo.ab2d9eb.png");
        op.setFontSize(66);
        op.setR(255);
        op.setG(255);
        op.setB(255);
        command.setOps(List.of(op));
        command.setUrl("https://testv.docbook.com.cn/customerTrans/692bec965a43d406e8ad68dee453a2f5/639f789f-188c70bbb4f-0006-e012-d30-d3e2c.mp4");
        command.setTargetUrls(List.of("/vc_watermark.mp4"));
        clipService.videoClip(command);
    }

    @Test
    public void testSpeed() {
        VideoClipsCommand command = new VideoClipsCommand();
        SpeedOp op = new SpeedOp();
        op.setStartTime(10 * 1000 * 1000);
        op.setEndTime(20 * 1000 * 1000);
        op.setValue("0.75");
        command.setOps(List.of(op));
        command.setUrl("/example.mp4");
        command.setTargetUrls(List.of("/vc.mp4"));
        clipService.videoClip(command);
    }

}
