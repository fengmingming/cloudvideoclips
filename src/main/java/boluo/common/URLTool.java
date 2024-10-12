package boluo.common;

import cn.hutool.core.util.StrUtil;

public class URLTool {

    public static RecorderConfig buildQueryObj(String query) {
        RecorderConfig queryObj = new RecorderConfig();
        if(StrUtil.isBlank(query)) {
            return queryObj;
        }
        String[] pairs = query.split("&");
        for(String pair : pairs) {
            String[] kv = pair.split("=");
            if(kv.length == 2) {
                queryObj.set(kv[0], kv[1]);
            }
        }
        return queryObj;
    }

}
