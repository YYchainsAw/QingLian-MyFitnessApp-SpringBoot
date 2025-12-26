package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yychainsaw.mapper.ChatGroupMapper;
import com.yychainsaw.mapper.GroupMemberMapper;
import com.yychainsaw.mapper.MessageMapper;
import com.yychainsaw.mapper.UserMapper;
import com.yychainsaw.pojo.dto.GroupCreateDTO;
import com.yychainsaw.pojo.entity.ChatGroup;
import com.yychainsaw.pojo.entity.GroupMember;
import com.yychainsaw.pojo.entity.Message;
import com.yychainsaw.pojo.vo.GroupListVO;
import com.yychainsaw.service.GroupService; // 需自行创建接口定义
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private ChatGroupMapper chatGroupMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 确保有事务注解
    public void addMember(Long groupId, UUID userId) {
        // 1. 校验用户是否存在 (建议先注释掉这行，排除 UserMapper 的问题，直接测试插入)
        // if (userMapper.selectById(userId) == null) {
        //    throw new RuntimeException("用户不存在");
        // }

        // 2. 检查是否已经是成员
        QueryWrapper<GroupMember> query = new QueryWrapper<>();
        query.eq("group_id", groupId).eq("user_id", userId);
        Long count = groupMemberMapper.selectCount(query);
        if (count > 0) {
            System.out.println("用户 " + userId + " 已经是群 " + groupId + " 的成员，跳过插入。");
            return;
        }

        // 3. 插入成员表
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());

        int rows = groupMemberMapper.insert(member);
        System.out.println("插入成员结果: " + rows + ", GroupID: " + groupId + ", UserID: " + userId);
    }

    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        QueryWrapper<GroupMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId);

        return groupMemberMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroup createGroup(GroupCreateDTO dto) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();

        // 1. 插入群组表
        ChatGroup group = new ChatGroup();
        group.setName(dto.getName());
        group.setOwnerId(userId);
        group.setAvatarUrl(dto.getAvatarUrl());
        group.setNotice(dto.getNotice());
        
        chatGroupMapper.insert(group); // MyBatisPlus 会自动回填 groupId

        // 2. 将创建者添加为群主
        GroupMember member = new GroupMember();
        member.setGroupId(group.getGroupId());
        member.setUserId(userId);
        member.setRole("OWNER");
        
        groupMemberMapper.insert(member);

        return group;
    }

    @Override
    public List<GroupListVO> getUserGroups() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        // 1. 查询该用户参与的所有成员记录
        QueryWrapper<GroupMember> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        List<GroupMember> members = groupMemberMapper.selectList(query);

        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 提取群组 ID
        List<Long> groupIds = members.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toList());

        // 3. 查询群组基本信息
        List<ChatGroup> groups = chatGroupMapper.selectBatchIds(groupIds);

        // 4. 【核心修改】封装成 VO 并填充 lastMessage
        List<GroupListVO> resultList = new ArrayList<>();

        for (ChatGroup group : groups) {
            GroupListVO vo = new GroupListVO();
            BeanUtils.copyProperties(group, vo); // 复制基本属性

            // --- 查询该群最后一条消息 ---
            QueryWrapper<Message> msgQuery = new QueryWrapper<>();
            msgQuery.eq("group_id", group.getGroupId())
                    .orderByDesc("sent_at") // 按时间倒序
                    .last("LIMIT 1");       // 只取第一条

            Message lastMsg = messageMapper.selectOne(msgQuery);

            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
                vo.setLastMessageTime(lastMsg.getSentAt().toString()); // 或者格式化一下
            } else {
                vo.setLastMessage(""); // 无消息
            }

            // (可选) 这里还可以查询未读数 unreadCount，逻辑比较复杂，需要对比 GroupReadStatus 表
            vo.setUnreadCount(0); // 暂时置为 0

            resultList.add(vo);
        }
        return resultList;
    }
}
