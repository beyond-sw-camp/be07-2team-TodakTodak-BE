package com.padaks.todaktodak.post.controller;

import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.post.dto.*;
import com.padaks.todaktodak.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("post")
public class PostController {
    private final PostService postService;

    @PostMapping("/create")
    public ResponseEntity<?> register(@ModelAttribute PostsaveDto dto){
        try {
            postService.create(dto);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post 등록 성공", null);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "post 등록 실패" + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> postList(Pageable pageable){
        Page<PostListDto> postListDtos = postService.postList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post 목록을 조회합니다.", postListDtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long id){
        try {
            PostDetailDto postDetail = postService.getPostDetail(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "Post 상세정보를 조회합니다.", postDetail);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<?> updatePost (@PathVariable Long id, @RequestBody PostUpdateReqDto dto){
        try{
            postService.updatePost(id, dto);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post가 성공적으로 업데이트 되었습니다.", id);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id){
        try {
            postService.deletePost(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post가 삭제되었습니다.",id);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    // 조회수 증가 및 조회
    @GetMapping("/detail/views/{id}")
    public ResponseEntity<?> getPostViews(@PathVariable Long id) {
        try {
            postService.incrementPostViews(id); // 조회수 증가
            Long views = postService.getPostViews(id); // 조회수 조회
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "조회수를 조회합니다.", views);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "조회수 조회 실패: " + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 좋아요 추가
    @PostMapping("/detail/like/{id}")
    public ResponseEntity<?> likePost(@PathVariable Long id) {
        try {
            postService.likePost(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "좋아요가 추가되었습니다.", null);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "좋아요 추가 실패: " + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 좋아요 취소
    @PostMapping("/detail/unlike/{id}")
    public ResponseEntity<?> unlikePost(@PathVariable Long id) {
        try {
            postService.unlikePost(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "좋아요가 취소되었습니다.", null);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "좋아요 취소 실패: " + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 좋아요 수 조회
    @GetMapping("/detail/{id}/likes")
    public ResponseEntity<?> getPostLikesCount(@PathVariable Long id) {
        try {
            Long likesCount = postService.getPostLikesCount(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "좋아요 수를 조회합니다.", likesCount);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "좋아요 수 조회 실패: " + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
