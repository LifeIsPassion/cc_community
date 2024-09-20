package com.cyt.community.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.mail.smtp.DigestMD5;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    /**
     * 返回一个uuid其中_以空值替代
     *
     * @return
     */
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("_","");
    }

    //MD5加密
    public static String MD5(String key){
        if(StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    //返回json字符串
    public static String getJson(int code, String msg, Map<String,Object> map){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code" ,code);
        jsonObject.put("msg",msg);
        if(map != null){
            for(String key:map.keySet()){
                jsonObject.put(key,map.get(key));
            }
        }
        return jsonObject.toString();
    }

    //
    public static String getJson(int code, String msg){
        return getJson(code,msg,null);
    }
    //
    public static String getJson(int code){
        return getJson(code,null,null);
    }

}
