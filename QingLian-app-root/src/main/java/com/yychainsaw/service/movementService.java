package com.yychainsaw.service;

import com.yychainsaw.pojo.dto.MovementDTO;
import com.yychainsaw.pojo.dto.MovementDifficultyDTO;
import com.yychainsaw.pojo.dto.PageBean;
import com.yychainsaw.pojo.vo.MovementAnalyticsVO;
import com.yychainsaw.pojo.vo.MovementVO;

import java.util.List;
import java.util.Map;

public interface movementService {
    void addMovement(MovementDTO movementDTO);

    PageBean<MovementVO> search(String keyword, Integer pageNum, Integer pageSize);

    void changeDifficultyLevel(MovementDifficultyDTO movementDTO);

    void deleteUnusedMovement();

    List<Map<String, Object>> countCategories();

    List<MovementVO> getHardcoreMovements();

    List<MovementAnalyticsVO> getMovementAnalytics();
}
