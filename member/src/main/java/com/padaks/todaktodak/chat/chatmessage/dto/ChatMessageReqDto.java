package com.padaks.todaktodak.chat.chatmessage.dto;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageReqDto {

    private Long chatRoomId; // 채팅방 id

    private String token; // 멤버 토큰

    private String contents; // 메시지 내용

    private String senderName; // 보낸 사람 이름

    private String memberEmail; // 보낸 사람 이메일

    public static ChatMessage toEntity(ChatRoom chatRoom,
                                       Member sender,
                                       String contents
                                       ){
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .contents(contents)
                .build();
    }

    public static ChatMessageReqDto fromEntity(ChatMessageReqDto dto ,ChatRoom chatRoom, Member sender) {
        return ChatMessageReqDto.builder()
                .chatRoomId(chatRoom.getId())
                .contents(dto.getContents())
                .senderName(sender.getName()) // senderName 추가
                .memberEmail(sender.getMemberEmail()) // memberEmail 추가
                .build();
    }
}
