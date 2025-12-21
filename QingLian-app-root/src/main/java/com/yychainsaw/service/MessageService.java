package com.yychainsaw.service;

import com.yychainsaw.pojo.dto.MessageSendDTO;
import com.yychainsaw.pojo.vo.MessageVO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MessageService {
    MessageVO sendMessage(MessageSendDTO dto);

    void markAsRead(UUID senderId);

    Long getUnreadCount();

    List<Map<String, Object>> getChatHistory(UUID uuid1);
}
