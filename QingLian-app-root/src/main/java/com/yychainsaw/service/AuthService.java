package com.yychainsaw.service;

import com.yychainsaw.pojo.dto.UserLoginDTO;
import com.yychainsaw.pojo.dto.UserRegisterDTO;
import com.yychainsaw.pojo.vo.TokenVO;

public interface AuthService {
    void register(UserRegisterDTO userRegisterDTO);

    TokenVO login(UserLoginDTO loginDTO);

    void logout(String token);

    String refreshToken(String oldToken);
}
