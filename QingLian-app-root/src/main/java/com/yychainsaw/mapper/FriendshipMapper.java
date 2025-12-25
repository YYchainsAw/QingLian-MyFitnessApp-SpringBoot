package com.yychainsaw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yychainsaw.pojo.entity.Friendship;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.FriendListVO;
import com.yychainsaw.pojo.vo.FriendPlanVO;
import com.yychainsaw.pojo.vo.FriendRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    List<FriendPlanVO> selectFriendsActivePlans(UUID userId);

    List<FriendRankingVO> selectFriendRankings(UUID userId);

    List<FriendListVO> selectFriendList(UUID userId);
}
