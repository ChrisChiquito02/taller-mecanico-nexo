package com.sistemam01m02;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.sistemam01m02",
    importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule identityPackagesHaveNoCycles =
      slices().matching("com.sistemam01m02.identity.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule domainIndependent =
      noClasses()
          .that()
          .resideInAPackage("..entity..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..api..", "..service..", "..repository..", "..dto..", "org.springframework..");

  @ArchTest
  static final ArchRule servicesIndependentOfHttp =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api..", "jakarta.servlet..", "org.springframework.web..");

  @ArchTest
  static final ArchRule controllersDoNotUseDatabase =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..repository..", "jakarta.persistence..", "org.springframework.jdbc..");

  @ArchTest
  static final ArchRule persistenceIndependentOfPresentation =
      noClasses()
          .that()
          .resideInAPackage("..repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api..", "..service..", "..dto..");
}
