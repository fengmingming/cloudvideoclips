package boluo.common;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResVo<T> {

    private int code;
    private String codeDesc;
    private T data;

    public boolean isSuccess() {
        return this.code == 0;
    }

    public static <T> ResVo<T> success(T data) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setData(data);
        return resVo;
    }

    public static <T> ResVo<T> success() {
        ResVo<T> resVo = new ResVo<>();
        return resVo;
    }

    public static <T> ResVo<T> error(int code, String codeDesc) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setCode(code);
        resVo.setCodeDesc(codeDesc);
        return resVo;
    }

    public static <T> ResVo<T> error(String codeDesc) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setCode(500);
        resVo.setCodeDesc(codeDesc);
        return resVo;
    }

}