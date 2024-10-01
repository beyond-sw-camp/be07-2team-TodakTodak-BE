package com.padaks.todaktodak.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentUpdateReqDto {
    //private Long id;
    private Long postId;
    private String doctorEmail;
    private String content;
    private LocalDateTime updateTime;
}
