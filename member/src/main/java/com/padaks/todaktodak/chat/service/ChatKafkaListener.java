package com.padaks.todaktodak.chat.service;

import com.padaks.todaktodak.chat.chatmessage.dto.ChatMessageReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaListener { // Kafka

    private final WebSocketService webSocketService;

    @KafkaListener(topics = "ChatTopic", groupId = "chat-group", containerFactory = "chatKafkaListenerContainerFactory")
    public void listenChatMessages(@Payload ChatMessageReqDto chatMessageReqDto) {
        log.info("Received message: {}", chatMessageReqDto);

        // Kafka로 수신한 메시지를 WebSocket을 통해 전달
        Long chatRoomId = chatMessageReqDto.getChatRoomId();
        String memberEmail = chatMessageReqDto.getMemberEmail(); // token을 통해 email을 얻는 로직 필요

        // WebSocket을 통해 메시지를 채팅방에 전달
        webSocketService.sendMessage(chatRoomId, memberEmail, chatMessageReqDto);
    }
}
