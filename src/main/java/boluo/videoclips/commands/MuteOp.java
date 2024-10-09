package boluo.videoclips.commands;

import boluo.videoclips.OpChain;
import org.bytedeco.javacv.Frame;

public class MuteOp extends Op{

    @Override
    public void doFilter(OpContext context, Frame frame, OpChain chain) {
        if(frame.image != null || frame.data != null) {
            chain.doFilter(context, frame);
        }
    }

}
