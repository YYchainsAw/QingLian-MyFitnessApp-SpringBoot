package com.yychainsaw.controller;

import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.pojo.dto.UserUpdateDTO;
import com.yychainsaw.pojo.vo.UserVO;
import com.yychainsaw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    // 获取当前用户信息
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        UserVO userVO = userService.getUserInfo();
        return Result.success(userVO);
    }

    @PutMapping("/update")
    public Result updateProfile(@RequestBody @Validated UserUpdateDTO updateDTO) {
        userService.updateProfile(updateDTO);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result deleteAccount() {
        userService.deleteUser();
        return Result.success();
    }

    /**
     * 操作 10: 查找昵称中包含特定关键字的用户
     * 优化：
     * 1. 校验 keyword 是否为空
     * 2. 逻辑下沉到 Service
     */
    @GetMapping("/search")
    public Result<List<UserVO>> searchUsers(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<UserVO> users = userService.searchUsers(keyword.trim());
        return Result.success(users);
    }

    // SQL #12: 用户的社交概览 (Dashboard)
    // GET /user/dashboard
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getSocialDashboard() {
        Map<String, Object> dashboard = userService.getUserSocialDashboard();
        return Result.success(dashboard);
    }


}
