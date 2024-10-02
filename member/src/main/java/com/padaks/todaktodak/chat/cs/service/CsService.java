package com.padaks.todaktodak.chat.cs.service;

import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.chat.chatroom.repository.ChatRoomRepository;
import com.padaks.todaktodak.chat.cs.domain.Cs;
import com.padaks.todaktodak.chat.cs.dto.CsCreateReqDto;
import com.padaks.todaktodak.chat.cs.dto.CsResDto;
import com.padaks.todaktodak.chat.cs.dto.CsUpdateReqDto;
import com.padaks.todaktodak.chat.cs.repository.CsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CsService {

    private final CsRepository csRepository;
    private final ChatRoomRepository chatRoomRepository;

    // 회원별 CS 조회
    public List<CsResDto> getCsByMemberId(Long memberId) {
        List<Cs> csList = csRepository.findByMemberIdAndDeletedAtIsNull(memberId);
        return csList.stream()
                .map(CsResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // CS 상담내역 생성
    public Cs createCs(CsCreateReqDto dto){
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(dto.getChatRoomId());

        for(Cs cs : chatRoom.getCsList()){ // 채팅방에 엮여있는 CS 리스트 순회
            if(cs.getDeletedAt() == null){
                // 삭제되지 않은 CS 내역이 이미 존재할 경우 하나의 채팅방에 2개이상의 CS가 존재하는 것으로 간주
                throw new IllegalStateException("해당 채팅방에 이미 상담내역이 작성되었습니다.");
            }
        }

        Cs cs = CsCreateReqDto.toEntity(dto, chatRoom);
        return csRepository.save(cs);
    }

    // CS 수정
    public Cs updateCs(CsUpdateReqDto dto){
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(dto.getChatRoomId());
        Cs cs = csRepository.findByIdOrThrow(dto.getId());
        cs.updateCs(dto, chatRoom); // cs에서 업데이트

        return csRepository.save(cs);
    }

    // CS id로 CS 조회
    public CsResDto detailCsByCsId(Long csId){
        Cs cs = csRepository.findByIdOrThrow(csId); // 삭제 되지 않은 CS중 id로 조회
        return CsResDto.fromEntity(cs);
    }

    // 채팅방 id로 CS 조회
    public List<CsResDto> detailCsByChatRoomId(Long chatRoomId){
        List<Cs> csList = csRepository.findByChatRoomIdAndDeletedAtIsNull(chatRoomId);
        return csList.stream()
                .map(CsResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // CS 삭제
    public void deleteCs(Long id){
        Cs cs = csRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("id에 해당하는 CS 내역이 존재하지 않습니다."));

        cs.setDeletedTimeAt(LocalDateTime.now());
        csRepository.save(cs);
    }

}
