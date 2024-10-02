package com.padaks.todaktodak.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.padaks.todaktodak.chat.service.WebSocketService;
import com.padaks.todaktodak.chat.chatmessage.dto.ChatMessageReqDto;
import com.padaks.todaktodak.config.JwtTokenProvider;
import com.padaks.todaktodak.config.StompHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
//@RequiredArgsConstructor
@RestController
public class WebSocketController {

//    private final WebSocketService webSocketService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KafkaTemplate<String, Object> chatKafkaTemplate;

    public WebSocketController(
//                                WebSocketService webSocketService,
                               JwtTokenProvider jwtTokenProvider,
                               @Qualifier("chatKafkaTemplate") KafkaTemplate<String, Object> chatKafkaTemplate) {
//        this.webSocketService = webSocketService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.chatKafkaTemplate = chatKafkaTemplate;
    }

    // 웹소켓 메시지를 특정 경로로 매핑한다.
    @MessageMapping("/{chatRoomId}") // /pub/1
    public void sendMessage(ChatMessageReqDto chatMessageReqDto,
                            @DestinationVariable(value = "chatRoomId") Long chatRoomId) throws JsonProcessingException {
        log.info("ChatMessageReqDto : {}", chatMessageReqDto);
        String memberEmail = jwtTokenProvider.getEmailFromToken(chatMessageReqDto.getToken());

//        webSocketService.sendMessage(chatRoomId, memberEmail, chatMessageReqDto); // stomp붙일때만

        // ===== KafKa ======
        // 클라이언트 메시지 전송 -> WebSocketController에서 Kafka 프로듀서로 보냄
        // Kafka가 메시지 ChatTopic에 저장하고, ChatKafkaListener가 해당 메시지 수신
        // ChatKafkaListener는 WebSocketService 호출해서 메시지를 웹소켓을 통해 클라이언트로 전달

        // Kafka 토픽에 메시지를 전송
        chatMessageReqDto.setMemberEmail(memberEmail); // 보낸 사람 설정
        chatKafkaTemplate.send("ChatTopic", chatMessageReqDto); // 카프카에 메시지 전송

    }


}
