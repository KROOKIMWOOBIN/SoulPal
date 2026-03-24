package com.soulpal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GroupRoomRequest {
    @NotBlank
    private String projectId;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    @Size(min = 2, max = 10, message = "캐릭터는 최소 2명, 최대 10명까지 초대할 수 있습니다.")
    private List<String> characterIds;
}
