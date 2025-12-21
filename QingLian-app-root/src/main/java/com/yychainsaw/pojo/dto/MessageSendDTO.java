package com.yychainsaw.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageSendDTO {
    @NotNull(message = "接收者不能为空")
    private String receiverId; // 接收者的 UUID 字符串

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
