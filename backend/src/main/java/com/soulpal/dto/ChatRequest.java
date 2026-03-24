package com.soulpal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "캐릭터 ID는 필수입니다.")
    @Size(max = 36, message = "캐릭터 ID가 올바르지 않습니다.")
    private String characterId;

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 2000, message = "메시지는 2000자를 초과할 수 없습니다.")
    private String message;

    private int historyCount = 10;

    /** true: 강제 웹 검색 / false: 키워드 자동 판단 */
    private boolean webSearch = false;
}
