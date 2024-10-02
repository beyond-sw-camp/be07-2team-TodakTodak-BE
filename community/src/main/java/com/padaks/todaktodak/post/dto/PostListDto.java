package com.padaks.todaktodak.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostListDto {
    private Long id;
    private String memberEmail;
    private String title;
    private String content;
    private Long likeCount;
    private Long viewCount;
    private LocalDateTime createdTimeAt;
}
