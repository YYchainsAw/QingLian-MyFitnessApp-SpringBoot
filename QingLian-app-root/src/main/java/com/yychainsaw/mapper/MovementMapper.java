package com.yychainsaw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yychainsaw.pojo.dto.MovementDTO;
import com.yychainsaw.pojo.dto.MovementDifficultyDTO;
import com.yychainsaw.pojo.entity.Movement;
import com.yychainsaw.pojo.vo.MovementAnalyticsVO;
import com.yychainsaw.pojo.vo.MovementVO;
import lombok.Data;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface MovementMapper extends BaseMapper<Movement> {

    List<MovementAnalyticsVO> getMovementAnalytics();

    List<MovementVO> getAllMovements();
}
