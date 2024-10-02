package com.padaks.todaktodak.chat.service;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.chat.chatmessage.dto.ChatMessageResDto;
import com.padaks.todaktodak.chat.chatmessage.repository.ChatMessageRepository;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.chat.chatroom.dto.ChatRoomListResDto;
import com.padaks.todaktodak.chat.chatroom.repository.ChatRoomRepository;
import com.padaks.todaktodak.chat.cs.repository.CsRepository;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.padaks.todaktodak.common.exception.exceptionType.MemberExceptionType.TODAK_ADMIN_ONLY;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final CsRepository csRepository;

    private static final Long ADMIN_ID = 1L; // Admin 고정 ID

    // 채팅방 생성
    public ChatRoom createChatRoom(){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByMemberEmailOrThrow(memberEmail);
        log.info("line:44 memberEmail {}", memberEmail);
        // 새로운 채팅방 생성, member와 admin의 1:1 채팅방
        ChatRoom chatRoom = ChatRoom.builder()
                .member(member)
                .recentChatTime(LocalDateTime.now()) // 채팅방 생성과 함께 최근채팅시간 update (null로 두지 않기 위해서)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    // 채팅방 모든 메시지 조회
    public List<ChatMessageResDto> getMessages(Long chatRoomId){
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        return chatMessageRepository.findByChatRoom(chatRoom)
                .stream()
                .map(ChatMessageResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 채팅방 삭제
    public void deleteChatRoom(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        chatRoom.setDeletedTimeAt(LocalDateTime.now()); // 채팅방 deletedAt = 현재시간
    }

    // 해당 회원이 속한 채팅방 리스트 (회원입장 채팅방 리스트)
    public List<ChatRoomListResDto> getMemberChatRoomList(){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("line 71 memberEmail : " + memberEmail);
        Member member = memberRepository.findByMemberEmailOrThrow(memberEmail);

        // 해당 회원이 속한 모든 채팅방 조회
        List<ChatRoom> chatRoomList = chatRoomRepository.findByMemberId(member.getId());

        List<ChatRoomListResDto> chatRoomListResDtoList = new ArrayList<>();
        ChatMessage lastMessage = null; // 채팅방 마지막 메시지

        for(ChatRoom chatRoom : chatRoomList){

            // 해당 채팅방에서 가장 마지막 채팅메시지 조회
            lastMessage = chatMessageRepository.findFirstByChatRoomOrderByCreatedAtDesc(chatRoom)
                    .orElse(null); // 메시지가 없으면 null 반환

            chatRoomListResDtoList.add(ChatRoomListResDto.fromEntity(chatRoom, lastMessage));
        }

        return chatRoomListResDtoList;
    }


    // 채팅방 리스트(admin입장 채팅방 리스트)
    public List<ChatRoomListResDto> getAdminChatRoomList(){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!memberEmail.equals("todak@test.com")){
            // todak admin만 접근가능
            throw new BaseException(TODAK_ADMIN_ONLY);
        }

        // 모든 채팅방 조회
        List<ChatRoom> chatRoomList = chatRoomRepository.findAll();

        List<ChatRoomListResDto> chatRoomListResDtoList = new ArrayList<>();
        ChatMessage lastMessage = null; // 채팅방 마지막 메시지

        for(ChatRoom chatRoom : chatRoomList){

            // 해당 채팅방에서 가장 마지막 채팅메시지 조회
            lastMessage = chatMessageRepository.findFirstByChatRoomOrderByCreatedAtDesc(chatRoom)
                    .orElse(null); // 메시지가 없으면 null 반환

            chatRoomListResDtoList.add(ChatRoomListResDto.fromEntity(chatRoom, lastMessage));
        }

        return chatRoomListResDtoList;

    }


}
