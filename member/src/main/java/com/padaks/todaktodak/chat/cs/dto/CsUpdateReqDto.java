package com.padaks.todaktodak.chat.cs.dto;

import com.padaks.todaktodak.chat.cs.domain.CsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CsUpdateReqDto {

    private Long id; // CS 상담 id

    private Long chatRoomId; // 채팅방 id

    private String csContents; // 상담내역

    private CsStatus csStatus; // 상담처리상태
}
