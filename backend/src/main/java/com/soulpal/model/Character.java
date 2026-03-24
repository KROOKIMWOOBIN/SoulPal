package com.soulpal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "characters", indexes = {
    @Index(name = "idx_char_user_project", columnList = "user_id, project_id"),
    @Index(name = "idx_char_user_project_time", columnList = "user_id, project_id, last_message_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(nullable = false)
    private String name;

    @Column(name = "relationship_id")
    private String relationshipId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_personalities", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "personality_id")
    private List<String> personalityIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_speech_styles", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "speech_style_id")
    private List<String> speechStyleIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_interests", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "interest_id")
    private List<String> interestIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_appearances", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "appearance_id")
    private List<String> appearanceIds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "is_favorite")
    @Builder.Default
    private boolean favorite = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
