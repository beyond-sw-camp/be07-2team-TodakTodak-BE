package com.padaks.todaktodak.chat.service;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.chat.chatmessage.dto.ChatMessageReqDto;
import com.padaks.todaktodak.chat.chatmessage.repository.ChatMessageRepository;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.chat.chatroom.repository.ChatRoomRepository;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class WebSocketService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    public void sendMessage(Long chatRoomId, String memberEmail, ChatMessageReqDto dto){
        // chat room 찾기
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(dto.getChatRoomId());

        // 최근 채팅시간 업데이트
        chatRoom.updateRecentChatTime(LocalDateTime.now());

        // 보낸 사람 찾기
        Member sender = memberRepository.findByMemberEmailOrThrow(memberEmail);

        ChatMessage chatMessage = ChatMessageReqDto.toEntity(chatRoom, sender, dto.getContents());
        chatMessageRepository.save(chatMessage); // 메시지 저장

        ChatMessageReqDto messageDto = ChatMessageReqDto.fromEntity(dto, chatRoom, sender);
        log.info("messageDto : {}", messageDto);
        messagingTemplate.convertAndSend("/sub/" + chatRoomId, messageDto);
    }
}
