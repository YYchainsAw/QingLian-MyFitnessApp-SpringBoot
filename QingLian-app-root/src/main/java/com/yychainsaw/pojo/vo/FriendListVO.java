package com.yychainsaw.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FriendListVO {
    private UUID userId;
    private String username;
    private  String nickname;
    private String avatarUrl;
    private String email;
    
    // 新增字段：用于前端展示会话状态
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
