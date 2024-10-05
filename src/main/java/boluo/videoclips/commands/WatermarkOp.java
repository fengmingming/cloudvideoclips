package boluo.videoclips.commands;

import boluo.videoclips.OpChain;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class WatermarkOp extends Op{

    private @Setter @Getter String fontName;
    private @Setter @Getter int fontType = Font.PLAIN;
    private @Setter @Getter int fontSize = 22;
    private @Setter @Getter String text;
    private @Setter @Getter String imageUrl;
    private @Setter @Getter int x = 50;
    private @Setter @Getter int y = 50;
    private @Setter @Getter int r = 255;
    private @Setter @Getter int g = 255;
    private @Setter @Getter int b = 255;
    private BufferedImage image;

    public void start() {
        if(x < 0) {
            x = 50;
        }
        if(y < 0) {
            y = 50;
        }
        if(r < 0 || r > 255) {
            r = 255;
        }
        if(g < 0 || g > 255) {
            g = 255;
        }
        if(b < 0 || b > 255) {
            b = 255;
        }
        if(StrUtil.isNotBlank(imageUrl)) {
            try {
                image = ImageIO.read(new URL(imageUrl));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        //视频帧才处理
        if(frame.image != null) {
            int x = getX();
            int y = getY();
            if(x < frame.imageWidth && y < frame.imageHeight && (StrUtil.isNotBlank(getText()) || this.image != null)) {
                BufferedImage bi = Java2DFrameUtils.toBufferedImage(frame);
                Graphics2D g2D = bi.createGraphics();
                if(StrUtil.isNotBlank(getText())) {
                    g2D.setFont(new Font(fontName, fontType, fontSize));
                    g2D.setColor(new Color(r,g,b));
                    g2D.drawString(getText(), x, y);
                }else {
                    g2D.drawImage(this.image, x, y, null);
                }
                g2D.dispose();
                frame = Java2DFrameUtils.toFrame(bi);
            }
        }
        chain.doFilter(context, frame);
    }

}
