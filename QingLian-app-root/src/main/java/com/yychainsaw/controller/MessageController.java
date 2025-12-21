package com.yychainsaw.controller;

import com.yychainsaw.pojo.dto.MessageSendDTO;
import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.pojo.vo.MessageVO;
import com.yychainsaw.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public Result sendMessage(@RequestBody @Validated MessageSendDTO dto) {

        MessageVO messageVO = messageService.sendMessage(dto);

        messagingTemplate.convertAndSendToUser(
                dto.getReceiverId(),
                "/queue/messages",
                messageVO
        );

        return Result.success(messageVO);
    }

    @PutMapping("/read/{senderId}")
    public Result markAsRead(@PathVariable String senderId) {
        messageService.markAsRead(UUID.fromString(senderId));
        return Result.success();
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount() {
        Long count = messageService.getUnreadCount();
        return Result.success(count);
    }

    @GetMapping("/history/{friendId}")
    public Result<List<Map<String, Object>>> getChatHistory(@PathVariable String friendId) {
        List<Map<String, Object>> history = messageService.getChatHistory(UUID.fromString(friendId));
        return Result.success(history);
    }
}
