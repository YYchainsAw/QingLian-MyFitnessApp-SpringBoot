package com.yychainsaw.controller;

import com.yychainsaw.pojo.dto.Result;
import com.yychainsaw.pojo.entity.User;
import com.yychainsaw.pojo.vo.FriendListVO;
import com.yychainsaw.pojo.vo.FriendPlanVO;
import com.yychainsaw.pojo.vo.FriendRankingVO;
import com.yychainsaw.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/friendships")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    @GetMapping
    public Result<List<FriendListVO>> getFriendList() {
        List<FriendListVO> friends = friendshipService.getFriendList();
        return Result.success(friends);
    }

    @PostMapping("/request")
    public Result sendFriendRequest(@RequestParam String friendId) {
        friendshipService.sendRequest(UUID.fromString(friendId));
        return Result.success();
    }

    @GetMapping("/request/pending")
    public Result<List<FriendListVO>> getFriendRequestList() {
        return Result.success(friendshipService.getPendingRequests());
    }

    @PutMapping("/{friendId}/accept")
    public Result acceptFriendRequest(@PathVariable String friendId) {
        friendshipService.acceptRequest(UUID.fromString(friendId));
        return Result.success();
    }

    @DeleteMapping("/{friendId}")
    public Result deleteFriend(@PathVariable String friendId) {
        friendshipService.deleteFriend(UUID.fromString(friendId));
        return Result.success();
    }

    @GetMapping("/plans")
    public Result<List<FriendPlanVO>> getFriendsActivePlans() {
        List<FriendPlanVO> plans = friendshipService.getFriendsActivePlans();
        return Result.success(plans);
    }

    @GetMapping("/rankings")
    public Result<List<FriendRankingVO>> getFriendRankings() {
        List<FriendRankingVO> rankings = friendshipService.getFriendRankings();
        return Result.success(rankings);
    }
}
