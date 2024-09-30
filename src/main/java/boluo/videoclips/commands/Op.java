package boluo.videoclips.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.bytedeco.javacv.Frame;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "opName")
@JsonSubTypes({@JsonSubTypes.Type(value = TrimOp.class, name = "trim")})
public abstract class Op {

    public abstract Frame doFilter(OpContext context, Frame frame);

    public void start() {}

    public void close() {}

}
