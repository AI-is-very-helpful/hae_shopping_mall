package com.hae.shop.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
            .importPackages("com.hae.shop");
    }

    @Test
    @DisplayName("도메인 레이어는 인프라 레이어에 의존하지 않아야 한다")
    void domain_shouldNotDependOnInfrastructure() {
        noClasses()
            .that().resideInAPackage("com.hae.shop.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.hae.shop.infrastructure..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("도메인 레이어는 Spring 프레임워크에 의존하지 않아야 한다")
    void domain_shouldNotDependOnSpring() {
        noClasses()
            .that().resideInAPackage("com.hae.shop.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("애플리케이션 레이어는 인프라 레이어에 직접 의존하지 않아야 한다")
    void application_shouldOnlyDependOnDomain() {
        classes()
            .that().resideInAPackage("com.hae.shop.application..")
            .and().haveNameMatching("^(?!.*Test).*$")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.hae.shop.domain..",
                "com.hae.shop.common..",
                "com.hae.shop.config..",
                "java..",
                "org.springframework.stereotype..",
                "org.springframework.transaction..",
                "org.springframework.cache..",
                "org.springframework.security..",
                "lombok.."
            )
            .check(importedClasses);
    }

    @Test
    @DisplayName("인터페이스 레이어는 도메인 레이어를 직접 사용할 수 있다")
    void interfaces_shouldOnlyUseDomainOrDtos() {
        classes()
            .that().resideInAPackage("com.hae.shop.interfaces..")
            .and().haveNameMatching("^(?!.*Test).*$")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.hae.shop.domain..",
                "com.hae.shop.common..",
                "com.hae.shop.application..",
                "com.hae.shop.interfaces..",
                "com.hae.shop.config..",
                "java..",
                "org.springframework..",
                "jakarta.validation..",
                "io.swagger.v3.oas.annotations..",
                "lombok.."
            )
            .check(importedClasses);
    }

    @Test
    @DisplayName("Port 인터페이스는 Domain 레이어에 위치해야 한다")
    void ports_shouldBeInDomainLayer() {
        classes()
            .that().haveNameMatching(".*Port$")
            .should().resideInAPackage("com.hae.shop.domain..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("도메인 모델은 JPA 엔티티 어노테이션을 가지지 않아야 한다")
    void domainModels_shouldNotHaveJpaAnnotations() {
        noClasses()
            .that().resideInAPackage("com.hae.shop.domain..")
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .check(importedClasses);
    }

    @Test
    @DisplayName("순환 의존성이 없어야 한다")
    void noCyclicDependencies() {
        slices()
            .matching("com.hae.shop.(**)..")
            .should().beFreeOfCycles()
            .check(importedClasses);
    }
}
