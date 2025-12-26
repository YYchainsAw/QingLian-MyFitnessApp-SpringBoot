package com.yychainsaw.service;

import com.yychainsaw.pojo.dto.GroupCreateDTO;
import com.yychainsaw.pojo.entity.ChatGroup;
import com.yychainsaw.pojo.entity.GroupMember;
import com.yychainsaw.pojo.vo.GroupListVO;

import java.util.List;
import java.util.UUID;

public interface GroupService {
    ChatGroup createGroup(GroupCreateDTO dto);

    // 在接口中添加方法定义
    void addMember(Long groupId, UUID userId);

    List<GroupMember> getGroupMembers(Long groupId);

    List<GroupListVO> getUserGroups();
}
