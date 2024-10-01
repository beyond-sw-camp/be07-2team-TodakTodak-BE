package com.padaks.todaktodak.comment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.comment.domain.Comment;
import com.padaks.todaktodak.comment.dto.CommentDetailDto;
import com.padaks.todaktodak.comment.dto.CommentReqDto;
import com.padaks.todaktodak.comment.dto.CommentSaveDto;
import com.padaks.todaktodak.comment.dto.CommentUpdateReqDto;
import com.padaks.todaktodak.comment.repository.CommentRepository;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.post.domain.Post;
import com.padaks.todaktodak.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MemberFeignClient memberFeignClient;

    //member 객체 리턴
    public MemberFeignDto getMemberInfo() {
        MemberFeignDto member = memberFeignClient.getMemberEmail();
        return member;
    }

    public Comment createComment(CommentSaveDto dto){
        MemberFeignDto member = getMemberInfo(); //현재 로그인한 사용자 정보
        String receiver; //comment 작성자 email; //fcm 받는 대상
        Comment savedComment;

        if (dto.getPostId() != null){
            Post post = postRepository.findById(dto.getPostId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 post입니다."));
            receiver = post.getMemberEmail(); //댓글이 없는 질문일 경운 알림 수신자 = 게시글 작성자
            String title = post.getTitle();
            String type = "COMMENT";
            Map<String, Object> messageData = new HashMap<>();
            //부모 댓글이 있는 경우
            if (dto.getParentId() != null){
                Comment parentComment = commentRepository.findById(dto.getParentId()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 부모 댓글입니다."));
                savedComment = commentRepository.save(dto.toEntity(post, parentComment, member.getMemberEmail()));
                receiver = parentComment.getDoctorEmail(); //댓글(1)에 댓글(2)을 생성하는 경우 수신자 = 댓글(1) 작성자
            }else {
                //부모댓글 없는 경우 새로운 댓글 생성
                savedComment = commentRepository.save(dto.toEntity(post, null, member.getMemberEmail()));
                type = "POST";
            }

            //fcm 메세지 데이터 객체 생성
            messageData.put("receiverEmail", receiver);
            messageData.put("postId", dto.getPostId());
            messageData.put("title", title);
            messageData.put("type", type);

            try {
                String message = objectMapper.writeValueAsString(messageData);
                kafkaTemplate.send("community-success", message);
                log.info("comment 등록 성공 메세지를 kafka로 전송: {}", message);
            } catch (JsonProcessingException e) {
                log.error("JSON 변환 오류: {}", e.getMessage());
                kafkaTemplate.send("community-fail", "JSON 변환 오류가 발생했습니다.");
            }


        }else {
            throw new IllegalArgumentException("답변을 위한 POST ID가 필요합니다.");
        }
        return savedComment;
    }

    public List<CommentDetailDto> getCommentByPostId(Long postId){
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(comment -> CommentDetailDto.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .doctorEmail(comment.getDoctorEmail())
                        .createdTimeAt(comment.getCreatedTimeAt())
                        .build())
                .collect(Collectors.toList());
    }

    public Page<CommentDetailDto> CommentListByDoctorId(String doctorEmail, Pageable pageable){
        Page<Comment> comments = commentRepository.findByDoctorEmail(doctorEmail, pageable);
        return comments.map(a->a.listFromEntity());
    }

    public void updateComment(Long id, CommentUpdateReqDto dto){
        MemberFeignDto member = getMemberInfo(); //현재 로그인 되어있는 사용자
        Comment comment = commentRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 comment입니다."));
        Post post = postRepository.findById(dto.getPostId()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 Post입니다."));
        String type = "COMMENT";
        Map<String, Object> messageData = new HashMap<>();
        //현재 로그인 되어있는 사용자가 comment의 작성자인 경우
        if (member.getMemberEmail() == comment.getDoctorEmail()){
            comment.update(dto);
            commentRepository.save(comment);
            //fcm 메세지 데이터 객체 생성

            messageData.put("receiverEmail", comment.getDoctorEmail());
            messageData.put("postId", post.getId());
            messageData.put("title", dto.getContent());
            messageData.put("type", type);

            try {
                String message = objectMapper.writeValueAsString(messageData);
                kafkaTemplate.send("community-success", message);
                log.info("comment 수정 성공 메세지를 kafka로 전송: {}", message);
            } catch (JsonProcessingException e) {
                log.error("JSON 변환 오류: {}", e.getMessage());
                kafkaTemplate.send("community-fail", "JSON 변환 오류가 발생했습니다.");
            }
        }else {
            throw new IllegalArgumentException("작성자 이외에는 수정할 수 없습니다.");
        }


    }

    public void deleteComment(Long id){
        MemberFeignDto member = getMemberInfo();
        Comment comment = commentRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 comment입니다."));
        if (member.getMemberEmail() == comment.getDoctorEmail()){
            comment.updateDeleteAt();
        }else {
            throw  new IllegalArgumentException("작성자 이외에는 삭제할 수 없습니다.");
        }

    }

}