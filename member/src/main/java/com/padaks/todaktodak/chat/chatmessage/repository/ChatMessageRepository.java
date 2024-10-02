package com.padaks.todaktodak.chat.chatmessage.repository;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 채팅방 기준으로 모든 메시지 찾아오기
    List<ChatMessage> findByChatRoom(ChatRoom chatRoom);

    // 채팅방 기준으로 모든 메시지를 최근 생성된 순으로 찾아오기
    List<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);

    // 특정 채팅방에서 가장 마지막 메시지 조회 (createdAt 내림차순으로 정렬하여 첫 번째 메시지만 가져오기)
    Optional<ChatMessage> findFirstByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
}
