package com.yychainsaw.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yychainsaw.pojo.dto.PostCreateDTO;
import com.yychainsaw.pojo.dto.PostUpdateDTO;
import com.yychainsaw.pojo.vo.PostVO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PostService {
    void createPost(PostCreateDTO dto);

    Page<PostVO> getPostFeed(int page, int size);

    void likePost(Long postId);

    void deletePost(Long postId);

    void updatePost(Long postId, PostUpdateDTO dto);

    List<Map<String, Object>> getInfluencers();

    List<Map<String, Object>> getPotentialFriends();

    List<Map<String, Object>> getStats();
}
