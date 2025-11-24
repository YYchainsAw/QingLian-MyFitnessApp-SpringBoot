package com.yychainsaw.controller;

import com.yychainsaw.pojo.Result;
import com.yychainsaw.pojo.User;
import com.yychainsaw.service.AuthService;
import com.yychainsaw.service.UserService;
import com.yychainsaw.utils.JwtUtil;
import com.yychainsaw.utils.Md5Util;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]{5,9}$") String username,@Pattern(regexp = "^\\S{6,}$") String password, String nickname) {
        User u = userService.findByUsername(username);

        if (u != null) {
            return Result.error("用户名已存在");
        }

        authService.register(username, password, nickname);
        return Result.success();
    }

    @PostMapping("/login")
    public Result login(@Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]{5,9}$") String username,@Pattern(regexp = "^\\S{6,}$") String password) {
        User LoginUser = userService.findByUsername(username);

        if (LoginUser == null || !Md5Util.getMD5String(password).equals(LoginUser.getPasswordHash())) {
            return Result.error("用户名或密码错误");
        }

        Map<String, Object> claim = new HashMap<>();
        claim.put("id", LoginUser.getId());
        claim.put("username", LoginUser.getUsername());
        String token = JwtUtil.genToken(claim);

        //存储到Redis中
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();

        operations.set(token, token, 1, TimeUnit.HOURS);

        return Result.success(token);
    }
}
