package com.yychainsaw.pojo.vo;

import com.yychainsaw.pojo.entity.ChatGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GroupListVO extends ChatGroup {
    // 扩展两个字段供前端展示
    private String lastMessage;      // 最后一条消息内容
    private String lastMessageTime;  // 最后一条消息时间
    private Integer unreadCount;     // 未读消息数 (可选，如果需要红点)
}
