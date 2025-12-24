package com.yychainsaw.controller;

import com.yychainsaw.pojo.dto.MovementDTO;
import com.yychainsaw.pojo.dto.MovementDifficultyDTO;
import com.yychainsaw.pojo.dto.PageBean;
import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.pojo.vo.MovementAnalyticsVO;
import com.yychainsaw.pojo.vo.MovementVO;
import com.yychainsaw.service.movementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movements")
public class MovementController {
    @Autowired
    private movementService movementService;

    @PostMapping("/add")
    public Result addMovement(@RequestBody @Validated MovementDTO movementDTO) {
        movementService.addMovement(movementDTO);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<PageBean<MovementVO>> searchMovements(@RequestParam(required = false) String keyword,
                                                        Integer pageNum,
                                                        Integer pageSize){

        PageBean<MovementVO> movements = movementService.search(keyword, pageNum, pageSize);
        return Result.success(movements);
    }

    @PostMapping("/change-difficulty")
    public Result changeDifficultyLevel(@RequestBody @Validated MovementDifficultyDTO movementDTO){
        movementService.changeDifficultyLevel(movementDTO);
        return Result.success();
    }

    @DeleteMapping("/deleteUnused")
    public  Result deleteMovement(){
        movementService.deleteUnusedMovement();
        return Result.success();
    }

    @GetMapping("/countCategories")
    public Result<List<Map<String, Object>>> countCategories() {
        List<Map<String, Object>> movements = movementService.countCategories();
        return Result.success(movements);
    }

    @GetMapping("/hardcore")
    public Result<List<MovementVO>> getHardcoreMovements() {
        return Result.success(movementService.getHardcoreMovements());
    }

    @GetMapping("/analytics")
    public Result<List<MovementAnalyticsVO>> getMovementAnalytics() {
        return Result.success(movementService.getMovementAnalytics());
    }

}
