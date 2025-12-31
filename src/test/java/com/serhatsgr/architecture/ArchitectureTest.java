package com.serhatsgr.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.serhatsgr", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    // Controller'lar sadece 'controller' paketinde olmalı
    // ve isimleri 'Controller' ile bitmeli.
    @ArchTest
    static final ArchRule controllers_should_reside_in_controller_package =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..controller..")
                    .orShould().resideInAPackage("..controller.Impl..");

    // Controller katmanı, Repository katmanına doğrudan erişemez (Service üzerinden geçmeli).
    @ArchTest
    static final ArchRule controllers_should_not_access_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");

    // Kural 3: Service sınıfları 'Service' veya 'ServiceImpl' ile bitmeli.
    @ArchTest
    static final ArchRule services_should_have_proper_names =
            classes().that().resideInAPackage("..service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("ServiceImpl");
}