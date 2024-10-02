package com.padaks.todaktodak.chat.chatroom.domain;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.common.domain.BaseTimeEntity;
import com.padaks.todaktodak.chat.cs.domain.Cs;
import com.padaks.todaktodak.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member; // 일반회원

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<Cs> csList = new ArrayList<>(); // 소프트 delete 때문에 1:N 관계로 변경

    private LocalDateTime recentChatTime; // 최근 채팅 시간

    public void updateRecentChatTime(LocalDateTime now) { // 최근 채팅시간 업데이트
        this.recentChatTime = now;
    }

}
