package com.soulpal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_char_time", columnList = "character_id, created_at"),
    @Index(name = "idx_msg_char_user", columnList = "character_id, is_user")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "character_id", nullable = false, length = 36)
    private String characterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_user")
    private boolean user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
