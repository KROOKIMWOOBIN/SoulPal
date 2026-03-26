package com.soulpal.service;

import com.soulpal.dto.ProjectRequest;
import com.soulpal.exception.BusinessException;
import com.soulpal.model.Project;
import com.soulpal.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService 테스트")
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;

    @InjectMocks ProjectService projectService;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id("proj-1")
                .name("테스트 프로젝트")
                .description("설명")
                .userId("user-1")
                .build();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("전체 프로젝트 조회 → 유저 소유 목록 반환")
    void getAll_returnsUserProjects() {
        given(projectRepository.findAllByUserIdOrderByCreatedAtDesc("user-1"))
                .willReturn(List.of(testProject));

        List<Project> result = projectService.getAll("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("proj-1");
    }

    @Test
    @DisplayName("프로젝트 없으면 빈 목록")
    void getAll_emptyList() {
        given(projectRepository.findAllByUserIdOrderByCreatedAtDesc("user-1"))
                .willReturn(List.of());

        List<Project> result = projectService.getAll("user-1");

        assertThat(result).isEmpty();
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ID로 프로젝트 조회 성공")
    void getById_success() {
        given(projectRepository.findByIdAndUserId("proj-1", "user-1"))
                .willReturn(Optional.of(testProject));

        Project result = projectService.getById("proj-1", "user-1");

        assertThat(result.getId()).isEqualTo("proj-1");
        assertThat(result.getName()).isEqualTo("테스트 프로젝트");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 → BusinessException")
    void getById_notFound() {
        given(projectRepository.findByIdAndUserId("ghost", "user-1"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById("ghost", "user-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("다른 유저 프로젝트 조회 → BusinessException")
    void getById_wrongUser() {
        given(projectRepository.findByIdAndUserId("proj-1", "other-user"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById("proj-1", "other-user"))
                .isInstanceOf(BusinessException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("프로젝트 생성 → 저장 후 반환")
    void create_success() {
        ProjectRequest req = new ProjectRequest();
        req.setName("새 프로젝트");
        req.setDescription("설명");

        given(projectRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Project result = projectService.create(req, "user-1");

        assertThat(result.getName()).isEqualTo("새 프로젝트");
        assertThat(result.getDescription()).isEqualTo("설명");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getId()).isNotNull();
        then(projectRepository).should().save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("프로젝트 수정 → 필드 업데이트 후 저장")
    void update_success() {
        ProjectRequest req = new ProjectRequest();
        req.setName("수정된 이름");
        req.setDescription("수정된 설명");

        given(projectRepository.findByIdAndUserId("proj-1", "user-1"))
                .willReturn(Optional.of(testProject));
        given(projectRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Project result = projectService.update("proj-1", req, "user-1");

        assertThat(result.getName()).isEqualTo("수정된 이름");
        assertThat(result.getDescription()).isEqualTo("수정된 설명");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 수정 → BusinessException")
    void update_notFound() {
        ProjectRequest req = new ProjectRequest();
        req.setName("수정");

        given(projectRepository.findByIdAndUserId("ghost", "user-1"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update("ghost", req, "user-1"))
                .isInstanceOf(BusinessException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("프로젝트 삭제 → delete 호출")
    void delete_success() {
        given(projectRepository.findByIdAndUserId("proj-1", "user-1"))
                .willReturn(Optional.of(testProject));

        projectService.delete("proj-1", "user-1");

        then(projectRepository).should().delete(testProject);
    }

    @Test
    @DisplayName("다른 유저 프로젝트 삭제 → BusinessException, delete 미호출")
    void delete_wrongUser() {
        given(projectRepository.findByIdAndUserId("proj-1", "other-user"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete("proj-1", "other-user"))
                .isInstanceOf(BusinessException.class);

        then(projectRepository).should(never()).delete(any());
    }
}
