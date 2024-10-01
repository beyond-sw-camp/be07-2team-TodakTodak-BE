package com.padaks.todaktodak.notification.dto;

import com.padaks.todaktodak.notification.domain.Type;
import lombok.Data;

@Data
public class ChildShareDto {
    private String memberEmail; //수신자 email
    private Long childId;   //자녀 id
    private String name;    //자녀이름
    private String sharer;  //공유자 이름
}
