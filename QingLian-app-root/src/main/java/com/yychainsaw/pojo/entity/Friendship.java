package com.yychainsaw.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yychainsaw.anno.FRState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName("friendships")
public class Friendship {

    @TableField("user_id")
    private UUID userId;

    @TableField("friend_id")
    private UUID friendId;

    @FRState
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
