package com.padaks.todaktodak.comment.domain;

import com.padaks.todaktodak.comment.dto.CommentDetailDto;
import com.padaks.todaktodak.comment.dto.CommentUpdateReqDto;
import com.padaks.todaktodak.common.domain.BaseTimeEntity;

import com.padaks.todaktodak.post.domain.Post;
import com.padaks.todaktodak.post.dto.PostDetailDto;
import com.padaks.todaktodak.report.domain.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Comment extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;
    @Column(nullable = false)
    private String doctorEmail; //post 작성자 / 의사
    @Column(nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    //댓글 대댓글, 대대댓글... 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    public CommentDetailDto listFromEntity(){
        return CommentDetailDto.builder()
                .id(this.id)
                .doctorEmail(this.doctorEmail)
                .content(this.content)
                .createdTimeAt(this.getCreatedTimeAt())
                .updatedTimeAt(this.getUpdatedTimeAt())
                .build();
    }

    public Comment update(CommentUpdateReqDto dto){
        this.content = dto.getContent();
        return this;
    }

}
