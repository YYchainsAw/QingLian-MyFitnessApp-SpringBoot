package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yychainsaw.mapper.UserMapper;
import com.yychainsaw.pojo.dto.UserUpdateDTO;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.UserVO;
import com.yychainsaw.service.UserService;
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public void registerUser(User user) {
        if (user.getUserId() == null) {
            user.setUserId(UUID.randomUUID());
        }
        userMapper.insert(user);
    }

    @Override
    public UserVO getUserInfo() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    @Transactional
    public void updateProfile(UserUpdateDTO dto) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        // MyBatis-Plus 的 updateById 默认只更新非空字段
        User user = new User();
        user.setUserId(userId);

        BeanUtils.copyProperties(dto, user);

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        userMapper.deleteById(userId);
    }

    @Override
    public void updateLastLoginTime() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        User user = new User();
        user.setUserId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public List<UserVO> searchUsers(String keyword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getNickname, keyword)
                .or()
                .like(User::getUsername, keyword)
                .select(User::getUserId, User::getUsername, User::getNickname, User::getAvatarUrl) // 优化：只查询需要的字段
                .last("LIMIT 20");

        List<User> users = userMapper.selectList(queryWrapper);


        return users.stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getGenderWeightStats() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("gender", "count(*) as count", "avg(weight) as totalWeight")
                    .groupBy("gender")
                    .isNotNull("gender");

        return userMapper.selectMaps(queryWrapper);
    }

    @Override
    public Map<String, Object> getUserSocialDashboard() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        return userMapper.selectUserSocialDashboard(userId);
    }

}
