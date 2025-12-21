package com.yychainsaw.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FriendshipService {
    void sendRequest(UUID friendId);

    void acceptRequest(UUID friendId);

    void deleteFriend(UUID friendId);

    List<Map<String, Object>> getFriendsActivePlans();

    List<Map<String, Object>> getFriendRankings();
}
