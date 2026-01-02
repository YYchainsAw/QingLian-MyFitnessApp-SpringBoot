package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yychainsaw.mapper.FriendshipMapper;
import com.yychainsaw.mapper.UserMapper;
import com.yychainsaw.pojo.entity.Friendship;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.FriendListVO;
import com.yychainsaw.pojo.vo.FriendPlanVO;
import com.yychainsaw.pojo.vo.FriendRankingVO;
import com.yychainsaw.service.FriendshipService;
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FriendshipServiceImpl implements FriendshipService {
    @Autowired
    private FriendshipMapper friendshipMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private String getFriendListKey(UUID userId) {
        return "user:friends:" + userId;
    }

    @Override
    public void sendRequest(UUID friendId) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }

        QueryWrapper<Friendship> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .nested(i -> i.eq("user_id", userId).eq("friend_id", friendId))
                .or()
                .nested(i -> i.eq("user_id", friendId).eq("friend_id", userId))
        );

        Friendship existing = friendshipMapper.selectOne(queryWrapper);

        if (existing != null) {
            if ("ACCEPTED".equals(existing.getStatus())) {
                throw new IllegalArgumentException("你们已经是好友了");
            } else if ("PENDING".equals(existing.getStatus())) {
                if (existing.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("你已经发送过申请，请勿重复发送");
                } else {
                    throw new IllegalArgumentException("对方已经向你发送了申请，请前往“好友申请”列表处理");
                }
            }
        }

        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setStatus("PENDING");
        friendshipMapper.insert(friendship);


        User friend = userMapper.selectById(friendId);

        if (friend != null) {
            String targetUsername = friend.getUsername();

            messagingTemplate.convertAndSendToUser(
                    targetUsername,
                    "/queue/messages",
                    "收到新的好友申请"
            );
            System.out.println("WebSocket消息已发送给用户: " + targetUsername);
        }
    }

    @Override
    public void acceptRequest(UUID friendId) {
        // userId: 当前操作人（接收者）
        // friendId: 对方（申请者）
        //数据库记录是 user_id(申请人) -> friend_id(接收人)
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        QueryWrapper<Friendship> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", friendId).eq("friend_id", userId);

        Friendship friendship = new Friendship();
        friendship.setStatus("ACCEPTED");
        int rows = friendshipMapper.update(friendship, wrapper);

        // 2. WebSocket 通知申请人 (如果数据库更新成功)
        if (rows > 0) {
            redisTemplate.delete(getFriendListKey(userId));
            redisTemplate.delete(getFriendListKey(friendId));
            User requester = userMapper.selectById(friendId);
            if (requester != null) {
                messagingTemplate.convertAndSendToUser(
                        requester.getUsername(), // 发给申请人
                        "/queue/messages",
                        "你的好友请求已被接受"
                );
                System.out.println("WebSocket消息已发送给申请人: " + requester.getUsername());
            }
        } else {
            System.out.println("警告: 未找到对应的好友申请记录，无法接受。");
        }
    }

    @Override
    public void deleteFriend(UUID friendId) {
        // 双向删除 (SQL #3)
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        QueryWrapper<Friendship> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w
                .nested(i -> i.eq("user_id", userId).eq("friend_id", friendId))
                .or()
                .nested(i -> i.eq("user_id", friendId).eq("friend_id", userId))
        );
        friendshipMapper.delete(wrapper);

        redisTemplate.delete(getFriendListKey(userId));
        redisTemplate.delete(getFriendListKey(friendId));
    }

    @Override
    public List<FriendPlanVO> getFriendsActivePlans() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        return friendshipMapper.selectFriendsActivePlans(userId);
    }

    @Override
    public List<FriendRankingVO> getFriendRankings() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        return friendshipMapper.selectFriendRankings(userId);
    }

    @Override
    public List<FriendListVO> getFriendList() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        String key = getFriendListKey(userId);

        String cacheValue = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(cacheValue)) {
            try {
                return objectMapper.readValue(cacheValue, new TypeReference<List<FriendListVO>>() {});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        List<FriendListVO> friends = friendshipMapper.selectFriendList(userId);

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(friends), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return friends;
    }

    @Override
    public List<FriendListVO> getPendingRequests() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        QueryWrapper<Friendship> wrapper = new QueryWrapper<>();

        wrapper.eq("friend_id", userId).eq("status", "PENDING");
        List<Friendship> pendingRequests = friendshipMapper.selectList(wrapper);

        if (pendingRequests.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<UUID> requesterIds = pendingRequests.stream()
                .map(Friendship::getUserId)
                .collect(java.util.stream.Collectors.toList());

        List<User> requesters = userMapper.selectBatchIds(requesterIds);

        return requesters.stream().map(user -> {
            FriendListVO vo = new FriendListVO();
            vo.setUserId(user.getUserId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());

            return vo;
        }).collect(java.util.stream.Collectors.toList());
    }
}
