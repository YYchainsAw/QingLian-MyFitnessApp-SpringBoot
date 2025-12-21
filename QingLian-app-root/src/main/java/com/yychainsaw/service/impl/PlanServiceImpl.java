package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yychainsaw.mapper.FriendshipMapper;
import com.yychainsaw.mapper.MessageMapper;
import com.yychainsaw.mapper.PlanMapper;
import com.yychainsaw.pojo.dto.PlanCreateDTO;
import com.yychainsaw.pojo.entity.Friendship;
import com.yychainsaw.pojo.entity.Message;
import com.yychainsaw.pojo.entity.Plan;
import com.yychainsaw.service.PlanService;
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {

    @Autowired
    private PlanMapper planMapper;
    @Autowired
    private FriendshipMapper friendshipMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPlanAndNotifyFriends(PlanCreateDTO dto) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        // 1. DTO -> Entity 转换
        Plan plan = new Plan();
        BeanUtils.copyProperties(dto, plan); // 属性拷贝
        plan.setUserId(userId);
        plan.setStatus("ACTIVE");
        // created_at, updated_at 由 MyBatis-Plus 自动填充
        planMapper.insert(plan);

        // 2. 查找所有好友 (双向查找)
        // 业务逻辑：好友关系可能是 (Me, Friend) 也可能是 (Friend, Me)
        LambdaQueryWrapper<Friendship> friendQuery = new LambdaQueryWrapper<>();
        friendQuery.eq(Friendship::getStatus, "ACCEPTED")
                .and(wrapper -> wrapper
                        .eq(Friendship::getUserId, userId)
                        .or()
                        .eq(Friendship::getFriendId, userId)
                );

        List<Friendship> friendships = friendshipMapper.selectList(friendQuery);

        // 3. 批量发送通知消息
        if (friendships == null || friendships.isEmpty()) {
            return;
        }

        String content = "我刚刚开始了一个新计划：" + plan.getTitle() + "，一起来健身吧！";

        for (Friendship f : friendships) {
            // 确定接收者 ID：如果我是 user_id，朋友就是 friend_id；反之亦然
            UUID friendId = f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId();

            Message msg = new Message();
            msg.setSenderId(userId);
            msg.setReceiverId(friendId);
            msg.setContent(content);
            msg.setIsRead(false);
            // sent_at 由 MyBatis-Plus 自动填充
            messageMapper.insert(msg);
        }
    }

    @Override
    public List<Map<String, Object>> getActivePlans() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        // SQL #8
        QueryWrapper<Plan> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", "ACTIVE")
                .ge("end_date", LocalDate.now());

        List<Plan> plans = planMapper.selectList(wrapper);

        // 简单转 Map，实际建议用 BeanUtils 转 VO
        return plans.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("planId", p.getPlanId());
            map.put("title", p.getTitle());
            map.put("status", p.getStatus());
            map.put("endDate", p.getEndDate());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void completePlan(Long planId) {
        // SQL #9
        Plan plan = new Plan();
        plan.setPlanId(planId);
        plan.setStatus("COMPLETED");
        planMapper.updateById(plan);
    }
}
