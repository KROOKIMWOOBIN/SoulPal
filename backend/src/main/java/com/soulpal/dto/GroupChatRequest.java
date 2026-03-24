package com.soulpal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupChatRequest {
    @NotBlank
    @Size(max = 36)
    private String roomId;

    @NotBlank
    @Size(max = 2000, message = "메시지는 2000자를 초과할 수 없습니다.")
    private String message;
}
