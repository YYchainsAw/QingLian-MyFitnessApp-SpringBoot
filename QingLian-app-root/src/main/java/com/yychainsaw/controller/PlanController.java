package com.yychainsaw.controller;

import com.yychainsaw.pojo.dto.PlanCreateDTO;
import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/plans")
public class PlanController {
    @Autowired
    private PlanService planService;

    /**
     * 创建健身计划
     * 业务逻辑：创建计划记录 -> 查找双向好友 -> 发送站内信通知
     */
    @PostMapping
    public Result createPlan(@RequestBody @Validated PlanCreateDTO dto) {

        planService.createPlanAndNotifyFriends(dto);

        return Result.success();
    }

    @GetMapping("/active")
    public Result<List<Map<String, Object>>> getActivePlans() {

        List<Map<String, Object>> plans = planService.getActivePlans();
        return Result.success(plans);
    }

    @PutMapping("/{planId}/complete")
    public Result completePlan(@PathVariable Long planId) {
        planService.completePlan(planId);
        return Result.success();
    }
}
