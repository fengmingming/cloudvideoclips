package boluo.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class RecorderConfig extends JSONObject {

    public Map<String, String> getMap(String key, Map<String, String> def) {
        String value = super.getStr(key);
        if(JSONUtil.isTypeJSONObject(value)) {
            JSONObject obj = JSONUtil.parseObj(value);
            def = new HashMap<>();
            for(String k : obj.keySet()) {
                def.put(k, obj.getStr(k));
            }
        }
        return def;
    }

    public Charset getCharset(String key, Charset def) {
        String value = super.getStr(key);
        if(StrUtil.isBlank(value)) {
            return def;
        }
        return Charset.forName(value);
    }

}
