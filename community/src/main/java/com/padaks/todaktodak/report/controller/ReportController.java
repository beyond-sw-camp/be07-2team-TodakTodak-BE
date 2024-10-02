package com.padaks.todaktodak.report.controller;

import com.padaks.todaktodak.comment.repository.CommentRepository;
import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.post.repository.PostRepository;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.report.dto.ReportSaveReqDto;
import com.padaks.todaktodak.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityNotFoundException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/report")
public class ReportController {
    private final ReportService reportService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @GetMapping("/get/member")
    private ResponseEntity<?> getMember(){
        try {
            MemberFeignDto dto = reportService.getMemberInfo();
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "member 정보를 가져왔습니다.", dto);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> register(@RequestBody ReportSaveReqDto dto){
        try{
            reportService.create(dto);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "신고 등록 성공", null);
            return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
        }catch (IllegalArgumentException | EntityNotFoundException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> reportList(@RequestParam(value = "type", required = false)String type,
                                        @PageableDefault(size = 10, sort = "createdTimeAt", direction = Sort.Direction.DESC)Pageable pageable){

        try {
            Page<?> reportList = reportService.reportList(pageable, type);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "신고 목록을 조회합니다.", reportList);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }

    }
}
