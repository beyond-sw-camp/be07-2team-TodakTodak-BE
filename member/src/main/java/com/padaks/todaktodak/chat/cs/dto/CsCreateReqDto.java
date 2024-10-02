package com.padaks.todaktodak.chat.cs.dto;

import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.chat.cs.domain.Cs;
import com.padaks.todaktodak.chat.cs.domain.CsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CsCreateReqDto {
    private Long chatRoomId; // 채팅방 id

    private String csContents; // 상담내용

    private CsStatus csStatus; // 처리상태

    public static Cs toEntity(CsCreateReqDto dto,
                              ChatRoom chatRoom){
        return Cs.builder()
                .chatRoom(chatRoom)
                .csContents(dto.getCsContents())
                .csStatus(dto.getCsStatus())
                .build();
    }
}
