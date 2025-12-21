package com.yychainsaw.service;

import com.yychainsaw.pojo.dto.PlanCreateDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlanService {
    void createPlanAndNotifyFriends(PlanCreateDTO dto);

    List<Map<String, Object>> getActivePlans();

    void completePlan(Long planId);
}
