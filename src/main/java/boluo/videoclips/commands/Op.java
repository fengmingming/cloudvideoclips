package boluo.videoclips.commands;

import boluo.videoclips.OpChain;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.bytedeco.javacv.Frame;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "opName")
@JsonSubTypes({@JsonSubTypes.Type(value = TrimOp.class, name = "trim"),
        @JsonSubTypes.Type(value = WatermarkOp.class, name = "watermark"),})
public abstract class Op {

    public abstract void doFilter(OpContext context, Frame frame, OpChain chain);

    public void start() {}

    public void close() {}

    public int order() {return 0;}

}
