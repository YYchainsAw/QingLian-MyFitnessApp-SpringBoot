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
import java.util.Map;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public MessageVO sendMessage(MessageSendDTO dto) {
        // 1. 构建实体对象
        UUID senderId = ThreadLocalUtil.getCurrentUserId();
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(UUID.fromString(dto.getReceiverId()));
        message.setContent(dto.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);

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

        User user = userMapper.selectById(UUID.fromString(dto.getReceiverId()));
        if (user != null) {
            String targetUsername = user.getUsername();
            messagingTemplate.convertAndSendToUser(
                    targetUsername,
                    "/queue/messages",
                    "收到新私信"
            );
        }


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
    public List<Map<String, Object>> getChatHistory(UUID friendId) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        return messageMapper.selectChatHistory(userId, friendId);
    }
}
