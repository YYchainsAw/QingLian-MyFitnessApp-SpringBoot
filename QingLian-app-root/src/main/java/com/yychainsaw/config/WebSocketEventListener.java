package com.yychainsaw.config; // 放在 config 包或 controller 包下

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            System.out.println("====== WebSocket 新连接 ======");
            System.out.println("系统认定的用户名为 (Principal Name): " + user.getName());
            System.out.println("==============================");
        } else {
            System.out.println("====== WebSocket 新连接 (未认证用户) ======");
        }
    }
}
