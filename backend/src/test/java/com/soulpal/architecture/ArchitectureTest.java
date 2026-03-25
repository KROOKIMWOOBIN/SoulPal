package com.soulpal.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit — 아키텍처 레이어 경계 강제 테스트.
 *
 * 이 테스트가 실패하면 레이어 위반이 발생한 것입니다:
 *   Controller → Service → Repository (단방향)
 *
 * AI 개발 지침:
 *   - Controller에서 Repository를 직접 호출하면 이 테스트가 실패합니다
 *   - Service에서 Controller를 역참조하면 이 테스트가 실패합니다
 *   - 새 클래스 추가 시 올바른 패키지(controller/service/repository)에 배치하세요
 */
@DisplayName("아키텍처 레이어 규칙 테스트")
class ArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.soulpal");
    }

    @Test
    @DisplayName("레이어드 아키텍처: Controller → Service → Repository (단방향)")
    void layeredArchitectureRules() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy("com.soulpal.controller..")
                .layer("Service").definedBy("com.soulpal.service..")
                .layer("Repository").definedBy("com.soulpal.repository..")
                .layer("Model").definedBy("com.soulpal.model..")
                .layer("DTO").definedBy("com.soulpal.dto..")
                .layer("Config").definedBy("com.soulpal.config..")
                .layer("Exception").definedBy("com.soulpal.exception..")
                .whereLayer("Controller").mayOnlyBeAccessedByLayers("Config")
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .check(classes);
    }

    @Test
    @DisplayName("Repository는 Controller 패키지에서 직접 접근 불가")
    void repositoryNotAccessedByController() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.soulpal.controller..")
                .should().accessClassesThat().resideInAPackage("com.soulpal.repository..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Controller 클래스는 @RestController 어노테이션 필수")
    void controllersShouldBeAnnotated() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.soulpal.controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);
        rule.check(classes);
    }

    @Test
    @DisplayName("Service 클래스는 @Service 어노테이션 필수")
    void servicesShouldBeAnnotated() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.soulpal.service..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class);
        rule.check(classes);
    }
}
