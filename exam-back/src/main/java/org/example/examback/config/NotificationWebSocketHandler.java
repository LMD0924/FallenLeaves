package org.example.examback.config;

/*
 * @Author:总会落叶
 * @Date:2025/11/9
 * @Description:
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.examback.entity.Notice;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket连接建立: " + session.getId());

        // 发送连接成功消息
        sendMessage(session, new WebSocketMessage("connected", "连接成功"));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);

        // 可以处理客户端的心跳或特定指令
        if ("ping".equals(payload)) {
            sendMessage(session, new WebSocketMessage("pong", ""));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket连接关闭: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        sessions.remove(session);
    }

    // 广播消息给所有连接的客户端
    public static void broadcastNotice(Notice notice) {
        WebSocketMessage message = new WebSocketMessage("new_notice", notice);
        broadcastMessage(message);
    }

    // 发送消息给特定用户
    public static void sendNoticeToUser(Integer userId, Notice notice) {
        WebSocketMessage message = new WebSocketMessage("new_notice", notice);
        sessions.forEach(session -> {
            try {
                // 这里可以根据session中的用户信息进行过滤
                // 简单实现：广播给所有用户
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                }
            } catch (IOException e) {
                System.err.println("发送消息失败: " + e.getMessage());
            }
        });
    }

    private static void broadcastMessage(WebSocketMessage message) {
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(message)));
                }
            } catch (IOException e) {
                System.err.println("广播消息失败: " + e.getMessage());
            }
        });
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    // WebSocket 消息包装类
    public static class WebSocketMessage {
        private String type;
        private Object data;
        private Long timestamp;

        public WebSocketMessage(String type, Object data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        // getter setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
