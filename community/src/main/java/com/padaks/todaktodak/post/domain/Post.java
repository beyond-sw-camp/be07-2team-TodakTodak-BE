package com.padaks.todaktodak.post.domain;

import com.padaks.todaktodak.comment.domain.Comment;
import com.padaks.todaktodak.common.domain.BaseTimeEntity;
import com.padaks.todaktodak.post.dto.PostListDto;
import com.padaks.todaktodak.post.dto.PostUpdateReqDto;
import com.padaks.todaktodak.report.domain.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;
    @Column(nullable = false)
    private String memberEmail;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String content;
    @Column
    private String postImgUrl;
    @ColumnDefault("0")
    private Long likeCount;
    @ColumnDefault("0")
    private Long viewCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Comment> commentList = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Report> reportList = new ArrayList<>();

    public PostListDto listFromEntity(){
        return PostListDto.builder()
                .id(this.id)
                .title(this.title)
                .memberEmail(this.memberEmail)
                .content(this.content)
                .likeCount(this.likeCount)
                .viewCount(this.viewCount)
                .createdTimeAt(this.getCreatedTimeAt())
                .build();
    }

    public void updateImage(String postImgUrl){
        this.postImgUrl = postImgUrl;
    }

    public Post update(PostUpdateReqDto dto){
        this.title = dto.getTitle();
        this.content = dto.getContent();
        return this;
    }

}
