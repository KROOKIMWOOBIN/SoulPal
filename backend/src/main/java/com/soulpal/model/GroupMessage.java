package com.soulpal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages", indexes = {
    @Index(name = "idx_group_msg_room_time", columnList = "room_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessage {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** null이면 사용자 메시지, 값이 있으면 해당 캐릭터의 응답 */
    @Column(name = "sender_character_id", length = 36)
    private String senderCharacterId;

    /** 발신자 표시 이름 (사용자: "나", 캐릭터: 캐릭터 이름) */
    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public boolean isUser() {
        return senderCharacterId == null;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
