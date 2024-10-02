package com.padaks.todaktodak.report.service;

import com.padaks.todaktodak.comment.domain.Comment;
import com.padaks.todaktodak.comment.repository.CommentRepository;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.post.domain.Post;
import com.padaks.todaktodak.post.repository.PostRepository;
import com.padaks.todaktodak.report.domain.Report;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.report.dto.ReportListResDto;
import com.padaks.todaktodak.report.dto.ReportSaveReqDto;
import com.padaks.todaktodak.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ReportService {
    private final MemberFeignClient memberFeignClient;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;

    //member 객체 리턴, 토큰 포함
    public MemberFeignDto getMemberInfo(){
        MemberFeignDto member = memberFeignClient.getMemberEmail();
        return member;
    }

    public Report create(ReportSaveReqDto dto){
        Post post = null;
        Comment comment = null;
        String reportedEmail = null;

        MemberFeignDto member = getMemberInfo(); //현재 로그인한 사용자 정보

        if (dto.getPostId() != null){
            post = postRepository.findById(dto.getPostId()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 post입니다."));
            reportedEmail = post.getMemberEmail();

        } else if (dto.getCommentId() != null) {
            comment = commentRepository.findById(dto.getCommentId()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 comment입니다."));
            reportedEmail = comment.getDoctorEmail();
        }


        String reporterEmail = member.getMemberEmail();
        log.info(reportedEmail);
        memberFeignClient.reportCountUp(reportedEmail);

        Report report = dto.toEntity(post, comment, reporterEmail, reportedEmail);
        report = reportRepository.save(report);

        return  report;
    }

    public Page<ReportListResDto> reportList(Pageable pageable, String type){
        Page<Report> reports;

        if ("post".equals(type)){
            reports = reportRepository.findByPostIsNotNull(pageable);
        } else if ("comment".equals(type)) {
            reports = reportRepository.findByCommentIsNotNull(pageable);
        }else {
            reports = reportRepository.findAll(pageable);
        }

        return reports.map(Report::listFromEntity);
    }
}
