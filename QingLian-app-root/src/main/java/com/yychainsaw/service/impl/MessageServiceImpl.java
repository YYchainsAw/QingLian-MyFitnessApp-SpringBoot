package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yychainsaw.mapper.MessageMapper;
import com.yychainsaw.mapper.UserMapper;
import com.yychainsaw.pojo.dto.MessageSendDTO;
import com.yychainsaw.pojo.entity.Message;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.MessageVO;
import com.yychainsaw.service.MessageService;
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;


    @Override
    public MessageVO sendMessage(MessageSendDTO dto) {
        UUID senderId = ThreadLocalUtil.getCurrentUserId();

        Message message = new Message();
        message.setContent(dto.getContent());
        message.setSenderId(senderId); // 获取当前登录用户

        if (dto.getGroupId() != null) {
            // 群聊设置
            message.setGroupId(dto.getGroupId());
            message.setReceiverId(null);
            message.setIsRead(true); // 群聊消息在 messages 表默认设为 true (因为状态由另一张表管理)
        } else {
            // 私聊设置
            message.setReceiverId(UUID.fromString(dto.getReceiverId()));
            message.setGroupId(null);
            message.setIsRead(false);
        }
        // 2. 插入数据库 (MyBatis 会自动回填 ID 到 message 对象中)
        messageMapper.insert(message);

        // 3. 构建并返回 VO (这是 Controller 需要的数据)
        MessageVO vo = new MessageVO();
        vo.setId(message.getMsgId()); // 确保 Mapper XML 配置了 useGeneratedKeys="true"
        vo.setSenderId(senderId.toString());
        vo.setSenderName(userMapper.selectById(senderId).getUsername());
        vo.setReceiverId(dto.getReceiverId());
        vo.setContent(message.getContent());
        vo.setSentAt(message.getSentAt());
        vo.setIsRead(message.getIsRead());

        return vo;
    }

    @Override
    public void markAsRead(UUID senderId) {
        // SQL #5
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        UpdateWrapper<Message> wrapper = new UpdateWrapper<>();
        wrapper.eq("sender_id", senderId)
                .eq("receiver_id", userId)
                .eq("is_read", false)
                .set("is_read", true);
        messageMapper.update(null, wrapper);
    }

    @Override
    public Long getUnreadCount() {
        // SQL #6
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", userId).eq("is_read", false);
        return messageMapper.selectCount(wrapper);
    }

    @Override
    public List<Message> getChatHistory(UUID friendId) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        return messageMapper.selectChatHistory(userId, friendId);
    }

    @Override
    public void markGroupAsRead(Long groupId, Long lastMsgId) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        messageMapper.markGroupAsRead(groupId, userId, lastMsgId);
    }

    @Override
    public List<Message> getGroupChatHistory(Long groupId) {
        QueryWrapper<Message> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.orderByDesc("sent_at");
        return messageMapper.selectList(query);
    }
}
