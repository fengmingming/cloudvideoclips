package boluo.videoclips.commands;

import boluo.videoclips.OpChain;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

@Setter
@Getter
public class WatermarkOp extends Op{

    private String fontName;
    private int fontType = Font.PLAIN;
    private int fontSize = 12;
    private String text;
    private int x;
    private int y;

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        //视频帧才处理
        if(frame.image != null) {
            BufferedImage image = Java2DFrameUtils.toBufferedImage(frame);
            Graphics2D g = image.createGraphics();
            g.setFont(new Font(fontName, fontType, fontSize));
            g.drawString(getText(), getX(), getY());
            g.dispose();
            frame = Java2DFrameUtils.toFrame(image);
        }
    }

}
