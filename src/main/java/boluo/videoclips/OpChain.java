package boluo.videoclips;

import boluo.videoclips.commands.Op;
import boluo.videoclips.commands.OpContext;
import org.bytedeco.javacv.Frame;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OpChain {

    private final List<Op> ops;
    private int index = 0;
    private final int length;

    public OpChain(List<Op> ops) {
        Objects.requireNonNull(ops, "ops is null");
        this.ops = ops;
        this.length = ops.size();
    }

    void restart() {
        this.index = 0;
    }

    Optional<Op> next() {
        if(index < length) {
            return Optional.of(ops.get(index++));
        }else {
            return Optional.empty();
        }
    }

    void innerFilter(Op op, OpContext context, Frame frame) {
        op.doFilter(context, frame, this);
     }

    public void doFilter(OpContext context, Frame frame) {
        Optional<Op> opOpt = next();
        if(opOpt.isPresent()) {
            innerFilter(opOpt.get(), context, frame);
        }
    }

    public void doFilter(OpContext context, List<Frame> frames) {
        Optional<Op> opOpt = next();
        if(opOpt.isPresent()) {
            frames.forEach(frame -> innerFilter(opOpt.get(), context, frame));
        }
    }

}
