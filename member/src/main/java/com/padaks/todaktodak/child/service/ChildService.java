package com.padaks.todaktodak.child.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.child.domain.Child;
import com.padaks.todaktodak.child.dto.ChildRegisterResDto;
import com.padaks.todaktodak.child.dto.ChildResDto;
import com.padaks.todaktodak.child.dto.ChildShareReqDto;
import com.padaks.todaktodak.child.dto.ChildUpdateReqDto;
import com.padaks.todaktodak.child.repository.ChildRepository;
import com.padaks.todaktodak.childparentsrelationship.domain.ChildParentsRelationship;
import com.padaks.todaktodak.childparentsrelationship.service.ChildParentsRelationshipService;
import com.padaks.todaktodak.common.dto.DtoMapper;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.common.service.AESUtil;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import com.padaks.todaktodak.util.S3ClientFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import javax.persistence.EntityNotFoundException;
import java.util.*;

import static com.padaks.todaktodak.common.exception.exceptionType.MemberExceptionType.*;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ChildService {
    private final DtoMapper dtoMapper;
    private final ChildRepository childRepository;
    private final MemberRepository memberRepository;
    private final ChildParentsRelationshipService childParentsRelationshipService;
    private final S3ClientFileUpload s3ClientFileUpload;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    // 환경 변수에서 암호화 키를 가져옴
    @Value("${encryption.secret-key}")
    private String secretKeyString;

    // 자녀 등록 (주민등록번호 암호화)
    public ChildRegisterResDto createChild(String name, String ssn, MultipartFile image) {
        String encryptedSsn;
        try {
            SecretKey secretKey = AESUtil.getSecretKeyFromString(secretKeyString);
            encryptedSsn = AESUtil.encrypt(ssn, secretKey); // 주민등록번호 암호화
        } catch (Exception e) {
            log.error("Error while encrypting SSN: ", e);
            throw new RuntimeException("주민등록번호 암호화에 실패했습니다.");
        }
        Optional<Child> optionalChild = childRepository.findBySsn(encryptedSsn);
        // 이미 등록된 자녀의 경우, 부모의 이름을 반환한다
        if (optionalChild.isPresent() && optionalChild.get().getDeletedAt() == null) {
            List<String> parentName = new ArrayList<>();
            List<Member> parents = childParentsRelationshipService.findParents(optionalChild.get());
            for (Member parent : parents) {
                parentName.add(parent.getName());
            }
            return ChildRegisterResDto.builder()
                    .childName(optionalChild.get().getName())
                    .parents(parentName)
                    .build();
        }
        String imageUrl = s3ClientFileUpload.upload(image);

        Child child = Child.builder()
                .name(name)
                .ssn(encryptedSsn)
                .profileImgUrl(imageUrl)
                .build();
        childRepository.save(child);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다"));
        childParentsRelationshipService.createRelationship(child, member);
        return ChildRegisterResDto.builder()
                .childName(child.getName())
                .build();

    }

    public void updateChild(ChildUpdateReqDto dto) {
        Child child = childRepository.findById(dto.getChildId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 자녀입니다"));
        child.updateName(dto.getName());
    }

    // 자녀 정보 조회 (주민등록번호 복호화)
    public List<ChildResDto> myChild() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다"));

        List<ChildParentsRelationship> childParentsRelationships = member.getChildParentsRelationshipList();
        List<ChildResDto> childList = new ArrayList<>();
        SecretKey secretKey;
        try {
            secretKey = AESUtil.getSecretKeyFromString(secretKeyString); // 복호화 키 준비
        } catch (Exception e) {
            log.error("Error while create secret key: ", e);
            throw new RuntimeException("복호화 키 준비에 실패했습니다.");
        }
        for (ChildParentsRelationship childParentsRelationship : childParentsRelationships) {
            Child child = childParentsRelationship.getChild();
            // 주민등록번호 복호화
            String decryptedSsn;
            try {
                decryptedSsn = AESUtil.decrypt(child.getSsn(), secretKey);
            } catch (Exception e) {
                log.error("Error while decrypting SSN: ", e);
                throw new RuntimeException("주민등록번호 복호화에 실패했습니다.");
            }
            ChildResDto childResDto = new ChildResDto().fromEntity(child, decryptedSsn);
            childList.add(childResDto);
        }
        return childList;

    }

    public void deleteChild(Long id) {
        Child child = childRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 자녀입니다"));
        child.delete();
    }

    // 자녀 공유 기능
    public void shareChild(ChildShareReqDto dto) {
        Child child = childRepository.findById(dto.getChildId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 자녀입니다"));
        Member shared = memberRepository.findById(dto.getSharedId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다"));
        childParentsRelationshipService.createRelationship(child, shared);

        String sharerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member sharer = memberRepository.findByMemberEmail(sharerEmail).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));


        // 메시지 데이터 객체 생성
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("memberEmail", shared.getMemberEmail()); // 받는 사람 = 자녀 공유 받는 사람
        messageData.put("childId", child.getId());
        messageData.put("childName", child.getName());
        messageData.put("sharer", sharer.getName()); //공유자


        // 객체를 JSON 문자열로 변환
        String message = null;
        try {
            message = objectMapper.writeValueAsString(messageData);
            log.info("전송할 메세지: {}", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("객체를 JSON 문자열로 변환하는데 실패했습니다.");
        }

        // Kafka로 메시지 전송
        kafkaTemplate.send("child-share", message);
        log.info("자녀 공유 성공 메시지를 Kafka로 전송: {}", message);
    }

    public ChildResDto getMyChild(Long id){
        Child child = childRepository.findById(id)
                .orElseThrow(() -> new BaseException(CHILD_NOT_FOUND));

        return dtoMapper.toChildResDto(child);
    }
}
