package com.padaks.todaktodak.chat.chatroom.dto;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomListResDto {
    // 회원입장 CS채팅 리스트에 보일 DTO
    // === ChatRoom 관련 ====
    private Long chatRoomId; // 채팅방 id

    private LocalDateTime ChatRoomCreatedAt; // 채팅방 생성일자

    private LocalDateTime recentChatTime; // 마지막 채팅시간

    // === ChatMessage 관련 ===
    private String lastMessage; // 마지막 메시지 내용

    private String senderName; // 메시지 송신자 이름

    private String senderProfileImgUrl; // 메시지 송신자 프로필 사진 url


    public static ChatRoomListResDto fromEntity(ChatRoom chatRoom,
                                                ChatMessage chatMessage){
        return ChatRoomListResDto.builder()
                .chatRoomId(chatRoom.getId())
                .ChatRoomCreatedAt(chatRoom.getCreatedAt())
                .recentChatTime(chatRoom.getRecentChatTime())
                .lastMessage(chatMessage != null ? chatMessage.getContents() : null)
                .senderName(chatMessage != null ? chatMessage.getSender().getName() : null)
                .senderProfileImgUrl(chatMessage != null ? chatMessage.getSender().getProfileImgUrl() : null)
                .build();
    }

}
