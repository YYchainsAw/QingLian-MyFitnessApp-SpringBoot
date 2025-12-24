package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yychainsaw.mapper.MovementMapper;
import com.yychainsaw.pojo.dto.MovementDTO;
import com.yychainsaw.pojo.dto.MovementDifficultyDTO;
import com.yychainsaw.pojo.dto.PageBean;
import com.yychainsaw.pojo.entity.Movement;
import com.yychainsaw.pojo.vo.MovementAnalyticsVO;
import com.yychainsaw.pojo.vo.MovementVO;
import com.yychainsaw.service.movementService;
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class movementServiceImpl implements movementService {
    @Autowired
    private MovementMapper movementMapper;

    @Override
    public void addMovement(MovementDTO movementDTO) {
        Movement movement = new Movement();

        movement.setTitle(movementDTO.getTitle());
        movement.setDescription(movementDTO.getDescription());
        movement.setCategory(movementDTO.getCategory());
        movement.setDifficultyLevel(movementDTO.getDifficultyLevel());

        movementMapper.insert(movement);
    }

    @Override
    public PageBean<MovementVO> search(String keyword, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        LambdaQueryWrapper<Movement> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Movement::getTitle, keyword)
                    .or().like(Movement::getDescription, keyword)
                    .or().like(Movement::getCategory, keyword)
            );
        }

        List<Movement> movements = movementMapper.selectList(queryWrapper);
        Page<Movement> page = (Page<Movement>) movements;
        long total = page.getTotal();

        List<MovementVO> movementVOs = movements.stream().map(m -> {
            MovementVO vo = new MovementVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());

        PageBean<MovementVO> pageBean = new PageBean<>();
        pageBean.setTotal(total);
        pageBean.setItems(movementVOs);

        return pageBean;
    }

    @Override
    public void changeDifficultyLevel(MovementDifficultyDTO movementDTO) {
        LambdaUpdateWrapper<Movement> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Movement::getCategory, movementDTO.getCategory())
                     .set(Movement::getDifficultyLevel, movementDTO.getDifficultyLevel());

        movementMapper.update(null, updateWrapper);
    }

    @Override
    public void deleteUnusedMovement() {
        LambdaQueryWrapper<Movement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Movement::getVideoUrl)
                    .eq(Movement::getDifficultyLevel, 0);

        movementMapper.delete(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> countCategories() {
        QueryWrapper<Movement> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("category", "COUNT(*) AS count")
                    .groupBy("category");

        return movementMapper.selectMaps(queryWrapper);
    }

    @Override
    public List<MovementVO> getHardcoreMovements() {
        LambdaQueryWrapper<Movement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(Movement::getDifficultyLevel, 4);

        List<Movement> list = movementMapper.selectList(queryWrapper);

        return list.stream().map(m -> {
            MovementVO vo = new MovementVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MovementAnalyticsVO> getMovementAnalytics() {
        return movementMapper.getMovementAnalytics();
    }

}
