package boluo.videoclips;

import boluo.Bootstrap;
import boluo.videoclips.commands.TrimOp;
import boluo.videoclips.commands.VideoClipsCommand;
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
        TrimOp op = new TrimOp();
        op.setStartTime(10 * 1000 * 1000);
        op.setEndTime(20 * 1000 * 1000);
        command.setOps(List.of(op));
        command.setUrl("https://testv.docbook.com.cn/customerTrans/692bec965a43d406e8ad68dee453a2f5/639f789f-188c70bbb4f-0006-e012-d30-d3e2c.mp4");
        command.setTargetUrls(List.of("/vc_.mp4"));
        clipService.videoClip(command);
    }

    public void testWaterMark() {
        
    }

}
