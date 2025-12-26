package com.yychainsaw.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yychainsaw.pojo.dto.MessageSendDTO;
import com.yychainsaw.pojo.dto.PageBean;
import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.pojo.entity.Message;
import com.yychainsaw.pojo.vo.MessageVO;
import com.yychainsaw.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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

        if (dto.getReceiverId() == null && dto.getGroupId() == null) {
            return Result.error("接收者或群组ID不能为空");
        }

        MessageVO messageVO = messageService.sendMessage(dto);

        if (dto.getGroupId() != null) {

            messagingTemplate.convertAndSend(
                    "/topic/group." + dto.getGroupId(),
                    messageVO
            );
        } else {

            messagingTemplate.convertAndSendToUser(
                    dto.getReceiverId(),
                    "/queue/messages",
                    messageVO
            );
        }

        return Result.success(messageVO);
    }

    // 标记群消息已读 (前端进入群聊页面时调用)
    @PutMapping("/group/read")
    public Result markGroupAsRead(@RequestParam Long groupId, @RequestParam Long lastMsgId) {
        messageService.markGroupAsRead(groupId, lastMsgId);
        return Result.success();
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
    public Result<PageBean<Message>> getChatHistory(
            @PathVariable String friendId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        // 1. 开启分页 (SQL 必须是 ORDER BY created_at DESC)
        PageHelper.startPage(pageNum, pageSize);

        // 2. 执行查询
        List<Message> history = messageService.getChatHistory(UUID.fromString(friendId));

        // 3. 使用 PageInfo 获取正确的 total (总条数)
        // PageHelper 会自动拦截 SQL 计算总数，必须这一步
        PageInfo<Message> pageInfo = new PageInfo<>(history);

        // 4. 核心修复：反转列表顺序 (为了前端展示习惯：旧 -> 新)
        List<Message> resultList = pageInfo.getList();
        Collections.reverse(resultList);

        // 5. 封装到自定义 PageBean
        PageBean<Message> pageBean = new PageBean<>(pageInfo.getTotal(), resultList);

        return Result.success(pageBean);
    }

    @GetMapping("/groups/{groupId}/history")
    public Result<PageBean<Message>> getGroupChatHistory(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        // 1. 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 2. 调用 Service 查询群消息
        List<Message> history = messageService.getGroupChatHistory(groupId);

        // 3. 获取分页信息
        PageInfo<Message> pageInfo = new PageInfo<>(history);

        // 4. 反转列表 (旧 -> 新) 供前端展示
        List<Message> resultList = pageInfo.getList();
        Collections.reverse(resultList);

        // 5. 封装返回
        PageBean<Message> pageBean = new PageBean<>(pageInfo.getTotal(), resultList);
        return Result.success(pageBean);
    }
}
