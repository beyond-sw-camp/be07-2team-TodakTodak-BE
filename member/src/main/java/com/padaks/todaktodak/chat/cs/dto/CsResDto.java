package com.padaks.todaktodak.chat.cs.dto;

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
public class CsResDto {

    private Long id;

    private String csContents;

    private String csStatus;

    private Long chatRoomId;

    public static CsResDto fromEntity(Cs cs){
        return CsResDto.builder()
                .id(cs.getId())
                .csContents(cs.getCsContents())
                .csStatus(cs.getCsStatus().getValue())
                .chatRoomId(cs.getChatRoom().getId())
                .build();
    }
}
