package com.yychainsaw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yychainsaw.mapper.PostMapper;
import com.yychainsaw.mapper.UserMapper;
import com.yychainsaw.pojo.dto.PostCreateDTO;
import com.yychainsaw.pojo.dto.PostUpdateDTO;
import com.yychainsaw.pojo.entity.Post;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.PostVO;
import com.yychainsaw.service.PostService;
import com.yychainsaw.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(PostCreateDTO dto) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        // --- 逻辑 13: 防止恶意刷帖 (Trigger -> Java) ---
        // 查询该用户最新的一条帖子
        LambdaQueryWrapper<Post> lastPostQuery = new LambdaQueryWrapper<>();
        lastPostQuery.eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT 1");
        Post lastPost = postMapper.selectOne(lastPostQuery);

        if (lastPost != null) {
            long secondsDiff = Duration.between(lastPost.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsDiff < 5) {
                throw new RuntimeException("操作太频繁，请 5 秒后再试");
            }
        }

        // --- 逻辑 12: VIP 初始赞 (Stored Procedure -> Java) ---
        int bonusLikes = 0;
        User user = userMapper.selectById(userId);
        if (user != null && user.getCreatedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            bonusLikes = 10; // 注册超过1年，初始赞 +10
        }

        // --- 插入帖子 ---
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(dto.getContent());
        // List<String> -> String[]
        if (dto.getImageUrls() != null) {
            post.setImageUrls(dto.getImageUrls().toArray(new String[0]));
        }
        post.setLikesCount(bonusLikes);

        postMapper.insert(post);
    }

    @Override
    public Page<PostVO> getPostFeed(int page, int size) {
        return postMapper.selectPostFeed(new Page<>(page, size));
    }

    @Override
    public void likePost(Long postId) {
        // 简单实现：直接 SQL 更新，并发高时可能不准，但符合原 SQL 逻辑
        // UPDATE posts SET likes_count = likes_count + 1 WHERE post_id = ?
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setLikesCount(post.getLikesCount() + 1);
            postMapper.updateById(post);
        }
    }

    @Override
    public void deletePost(Long postId) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();
        query.eq(Post::getPostId, postId).eq(Post::getUserId, userId);
        int deleted = postMapper.delete(query);
        if (deleted == 0) {
            throw new RuntimeException("删除失败：帖子不存在或无权删除");
        }
    }

    @Override
    public void updatePost(Long postId, PostUpdateDTO dto) {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        // 1. 检查帖子是否存在且属于该用户
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();
        query.eq(Post::getPostId, postId)
                .eq(Post::getUserId, userId);

        Post post = postMapper.selectOne(query);
        if (post == null) {
            throw new RuntimeException("修改失败：帖子不存在或无权修改");
        }

        // 2. 更新内容
        post.setContent(dto.getContent());
        // 如果有图片修改逻辑：post.setImageUrls(...)

        postMapper.updateById(post);
    }

    @Override
    public List<Map<String, Object>> getInfluencers() {
        return postMapper.selectActiveInfluencers();
    }

    @Override
    public List<Map<String, Object>> getPotentialFriends() {
        UUID userId = ThreadLocalUtil.getCurrentUserId();
        return postMapper.selectPotentialFriends(userId);
    }

    @Override
    public List<Map<String, Object>> getStats() {
        return postMapper.selectGenderWeightStats();
    }
}
