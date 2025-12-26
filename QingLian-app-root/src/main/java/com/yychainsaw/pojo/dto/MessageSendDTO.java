package com.yychainsaw.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageSendDTO {
    // 移除 @NotNull，因为群聊时不需要 receiverId
    private String receiverId;


    private Long groupId;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
