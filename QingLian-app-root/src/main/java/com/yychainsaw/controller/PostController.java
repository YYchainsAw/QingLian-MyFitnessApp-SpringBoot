package com.yychainsaw.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yychainsaw.pojo.dto.PostCreateDTO;
import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.pojo.vo.PostVO;
import com.yychainsaw.service.PostService;
import com.yychainsaw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/community")
public class PostController {

    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;

    // 操作 2: 发布帖子 (包含防刷帖和VIP赞逻辑)
    @PostMapping("/posts")
    public Result createPost(@RequestBody @Validated PostCreateDTO dto) {
        postService.createPost(dto);
        return Result.success();
    }

    // 操作 4: 分页获取 Feed 流
    @GetMapping("/feed")
    public Result<Page<PostVO>> getFeed(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.getPostFeed(page, size));
    }

    // 操作 6: 点赞
    @PostMapping("/posts/{postId}/like")
    public Result likePost(@PathVariable Long postId) {
        postService.likePost(postId);
        return Result.success();
    }

    // 操作 8: 删除帖子
    @DeleteMapping("/posts/{postId}")
    public Result deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return Result.success();
    }

    // 操作 11: 活跃达人榜
    @GetMapping("/influencers")
    public Result<List<Map<String, Object>>> getInfluencers() {
        return Result.success(postService.getInfluencers());
    }

    // 操作 14: 潜在好友推荐
    @GetMapping("/recommend-friends")
    public Result<List<Map<String, Object>>> getPotentialFriends() {
        return Result.success(postService.getPotentialFriends());
    }

    // 操作 15: 数据统计 (后台用)
    @GetMapping("/stats/gender-weight")
    public Result<List<Map<String, Object>>> getStats() {
        List<Map<String, Object>> stats = userService.getGenderWeightStats();
        return Result.success(stats);
    }
}
