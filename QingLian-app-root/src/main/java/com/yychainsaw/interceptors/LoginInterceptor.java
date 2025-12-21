package com.yychainsaw.interceptors;

import com.yychainsaw.utils.JwtUtil;
import com.yychainsaw.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 Header
        String token = request.getHeader("Authorization");

        // 2. 【新增】兼容处理：如果 Token 为空直接放行(或报错)，如果带 Bearer 前缀则去掉
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " (7个字符)
        }

        try {
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);

            if (redisToken == null) {
                throw new RuntimeException();
            }

            Map<String, Object> claims = JwtUtil.parseToken(token);

            ThreadLocalUtil.set(claims);

            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
