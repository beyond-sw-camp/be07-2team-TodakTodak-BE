package com.padaks.todaktodak.report.dto;

import com.padaks.todaktodak.comment.domain.Comment;
import com.padaks.todaktodak.post.domain.Post;
import com.padaks.todaktodak.report.domain.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportSaveReqDto {
    private String reason;
    private Long postId;
    private Long commentId;

    public Report toEntity(Post post, Comment comment, String reporterEmail, String reportedEmail){
        return Report.builder()
                .reporterEmail(reporterEmail)
                .reportedEmail(reportedEmail)
                .reason(this.reason)
                .post(post)
                .comment(comment)
                .build();
    }
}
