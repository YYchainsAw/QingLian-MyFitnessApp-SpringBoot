package com.yychainsaw.controller;

import com.yychainsaw.pojo.dto.Result;
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

    @PostMapping("/request")
    public Result sendFriendRequest(@RequestParam String friendId) {
        friendshipService.sendRequest(UUID.fromString(friendId));
        return Result.success();
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
    public Result<List<Map<String, Object>>> getFriendsActivePlans() {
        List<Map<String, Object>> plans = friendshipService.getFriendsActivePlans();
        return Result.success(plans);
    }

    @GetMapping("/rankings")
    public Result<List<Map<String, Object>>> getFriendRankings() {
        List<Map<String, Object>> rankings = friendshipService.getFriendRankings();
        return Result.success(rankings);
    }
}
