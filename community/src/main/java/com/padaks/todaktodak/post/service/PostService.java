package com.padaks.todaktodak.post.service;

import com.padaks.todaktodak.comment.dto.CommentDetailDto;
import com.padaks.todaktodak.comment.service.CommentService;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.post.domain.Post;
import com.padaks.todaktodak.post.dto.*;
import com.padaks.todaktodak.post.repository.PostRepository;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.util.S3ClientFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PostService {
    private final MemberFeignClient memberPostFeignClient;
    private final PostRepository postRepository;
    private final S3ClientFileUpload s3ClientFileUpload;
    private final CommentService commentService;
    @Qualifier("7") // Redis 퀄리파이어 설정
    private final RedisTemplate<String, Object> redisTemplate;

    //member 객체 리턴, 토큰 포함
    public MemberFeignDto getMemberInfo(){
        MemberFeignDto member = memberPostFeignClient.getMemberEmail();
        return member;
    }

    public void create(PostsaveDto dto){

        MultipartFile postImage = dto.getPostImage();
        MemberFeignDto member = getMemberInfo();
        String memberEmail = member.getMemberEmail();


        String imageUrl = s3ClientFileUpload.upload(postImage);

        Post post = dto.toEntity(imageUrl, memberEmail);
        postRepository.save(post);
    }

    public Page<PostListDto> postList(Pageable pageable){
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.map(a->a.listFromEntity());
    }

    public PostDetailDto getPostDetail(Long id){
        Post post = postRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("존재하지 않는 post입니다."));

        // 조회수 증가 로직 추가
        incrementPostViews(id);
        Long viewCount = getPostViews(id);
        Long likeCount = getPostLikesCount(id);

        List<CommentDetailDto> comments = commentService.getCommentByPostId(id);
        PostDetailDto postDetailDto = PostDetailDto.fromEntity(post, comments, viewCount, likeCount);
        return postDetailDto;
    }

    @Transactional
    public void updatePost(Long id, PostUpdateReqDto dto){
        Post post = postRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 Post입니다."));
        MultipartFile image = dto.getPostImg();
        if (image != null && !image.isEmpty()){
            String imageUrl = s3ClientFileUpload.upload(image);
            post.updateImage(imageUrl);
        }
        post.update(dto);
        postRepository.save(post);
    }

    public void deletePost(Long id){
        Post post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 post입니다."));
        post.updateDeleteAt();
    }

    // Redis 조회수 증가 로직
    public void incrementPostViews(Long postId) {
        MemberFeignDto member = getMemberInfo();
        String memberEmail = member.getMemberEmail();

        String key = "post:views:" + postId;
        String userKey = "post:views:users:" + postId;

        Boolean hasViewed = redisTemplate.opsForSet().isMember(userKey, memberEmail);
        if (!Boolean.TRUE.equals(hasViewed)) {
            redisTemplate.opsForValue().increment(key, 1);  // 조회수 1 증가
//            redisTemplate.opsForSet().add(userKey, memberEmail);  // 중복 방지용 유저 이메일 저장 추후 추가할 수 있음
        }
    }

    // Redis 조회수 조회
    public Long getPostViews(Long postId) {
        String key = "post:views:" + postId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();  // Integer를 Long으로 변환
        } else if (value instanceof Long) {
            return (Long) value;
        } else {
            return 0L;
        }
    }


    // Redis 좋아요 추가
    public void likePost(Long postId) {
        MemberFeignDto member = getMemberInfo();
        String memberEmail = member.getMemberEmail();

        String key = "post:likes:" + postId;
        String userKey = "post:likes:users:" + postId;

        Boolean hasLiked = redisTemplate.opsForSet().isMember(userKey, memberEmail);
        if (!Boolean.TRUE.equals(hasLiked)) {
            redisTemplate.opsForSet().add(key, memberEmail);  // 좋아요 누른 사용자 저장
            redisTemplate.opsForSet().add(userKey, memberEmail);  // 중복 방지용 이메일 저장
        }
    }

    // Redis 좋아요 취소
    public void unlikePost(Long postId) {
        MemberFeignDto member = getMemberInfo();
        String memberEmail = member.getMemberEmail();

        String key = "post:likes:" + postId;
        String userKey = "post:likes:users:" + postId;

        Boolean hasLiked = redisTemplate.opsForSet().isMember(userKey, memberEmail);
        if (Boolean.TRUE.equals(hasLiked)) {
            redisTemplate.opsForSet().remove(key, memberEmail);  // 좋아요 취소
            redisTemplate.opsForSet().remove(userKey, memberEmail);  // 중복 방지 이메일 제거
        }
    }

    // Redis 좋아요 수 조회
    public Long getPostLikesCount(Long postId) {
        String key = "post:likes:" + postId;
        Object value = redisTemplate.opsForSet().size(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();  // Integer를 Long으로 변환
        } else if (value instanceof Long) {
            return (Long) value;
        } else {
            return 0L;
        }
    }
}

