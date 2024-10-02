package com.padaks.todaktodak.post.dto;

import com.padaks.todaktodak.comment.dto.CommentDetailDto;
import com.padaks.todaktodak.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostDetailDto {
    private Long id;
    private String memberEmail;
    private String title;
    private String content;
    private String postImgUrl;
    private Long likeCount;
    private Long viewCount;
    private LocalDateTime createdTimeAt;
    private LocalDateTime updatedTimeAt;
    private List<CommentDetailDto> comments;

    public static PostDetailDto fromEntity(Post post, List<CommentDetailDto> comments, Long viewCount, Long likeCount ){
        return PostDetailDto.builder()
                .id(post.getId())
                .memberEmail(post.getMemberEmail())
                .title(post.getTitle())
                .content(post.getContent())
                .postImgUrl(post.getPostImgUrl())
                .likeCount(likeCount)
                .viewCount(viewCount)
                .createdTimeAt(post.getCreatedTimeAt())
                .updatedTimeAt(post.getUpdatedTimeAt())
                .comments(comments)
                .build();
    }

}

