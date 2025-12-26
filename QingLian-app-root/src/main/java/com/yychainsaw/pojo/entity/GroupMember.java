package com.yychainsaw.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName("group_members")
public class GroupMember {
    private Long groupId;
    private UUID userId;
    private String role; // OWNER, ADMIN, MEMBER
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime joinedAt;
}
