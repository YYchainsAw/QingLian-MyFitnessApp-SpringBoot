package com.yychainsaw.service;

import com.yychainsaw.pojo.dto.UserUpdateDTO;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.UserVO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserService {
    User findByUsername(String username);

    void registerUser(User user);// 仅供 AuthService 调用底层保存

    UserVO getUserInfo();

    void updateProfile(UserUpdateDTO userUpdateDTO);

    void deleteUser();

    void updateLastLoginTime();

    List<UserVO> searchUsers(String trim);

    List<Map<String, Object>> getGenderWeightStats();

    Map<String, Object> getUserSocialDashboard();

}
