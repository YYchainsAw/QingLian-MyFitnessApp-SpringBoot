package com.yychainsaw.utils;

import java.util.Map;
import java.util.UUID;

/**
 * ThreadLocal 工具类
 */

@SuppressWarnings("all")
public class ThreadLocalUtil {
    //提供ThreadLocal对象,
    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    //根据键获取值
    public static <T> T get(){
        return (T) THREAD_LOCAL.get();
    }
	
    //存储键值对
    public static void set(Object value){
        THREAD_LOCAL.set(value);
    }


    //清除ThreadLocal 防止内存泄漏
    public static void remove(){
        THREAD_LOCAL.remove();
    }

    public static UUID getCurrentUserId() {
        Map<String, Object> claims = get();
        if (claims != null && claims.get("id") != null) {
            return UUID.fromString((String) claims.get("id"));
        }
        throw new RuntimeException("未获取到登录用户信息");
    }
}
