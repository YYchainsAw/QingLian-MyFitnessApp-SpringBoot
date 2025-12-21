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

    // 复合主键在 MP 中通常不加 @TableId，直接作为普通字段处理
    // 操作时使用 wrapper.eq("user_id", uid).eq("friend_id", fid)
    @TableField("user_id")
    private UUID userId;

    @TableField("friend_id")
    private UUID friendId;

    @FRState
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 新增：记录状态变更时间（如接受好友请求的时间）
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
