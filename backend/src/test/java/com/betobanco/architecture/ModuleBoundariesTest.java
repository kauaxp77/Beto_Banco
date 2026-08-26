package com.betobanco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundariesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.betobanco");
    }

    @Test
    void nenhumModuloAcessaEntityOuRepositoryDeOutro() {
        for (String modulo : new String[]{
                "users", "auth", "students", "catalog", "entitlements",
                "payments", "webhooks", "email", "audit", "dashboard"}) {
            ArchRule regra = noClasses()
                    .that().resideOutsideOfPackage("com.betobanco." + modulo + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.betobanco." + modulo + ".entity..",
                            "com.betobanco." + modulo + ".repository..")
                    .because("modulos so podem se comunicar pelo pacote api/ do outro modulo");
            regra.allowEmptyShould(true).check(classes);
        }
    }

    @Test
    void nenhumControllerAceitaUserIdVindoDoCliente() {
        ArchRule regra = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should(ArchConditions.temParametroDeIdentidade())
                .because("a identidade vem do token, nunca do cliente");
        regra.allowEmptyShould(true).check(classes);
    }

    @Test
    void nenhumControllerRetornaEntidadeJpa() {
        ArchRule regra = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should(ArchConditions.retornaClasseAnotadaComEntity())
                .because("controllers devolvem DTO, nunca @Entity");
        regra.allowEmptyShould(true).check(classes);
    }

    static class ArchConditions {

        static com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
        temParametroDeIdentidade() {
            return new com.tngtech.archunit.lang.ArchCondition<>("aceitar userId do cliente") {

                private static final java.util.Set<String> PROIBIDOS =
                        java.util.Set.of("userid", "user_id", "studentid", "student_id",
                                "alunoid", "aluno_id");

                private boolean nomeProibido(String valor) {
                    return valor != null
                            && PROIBIDOS.contains(valor.toLowerCase().replace("-", "_"));
                }

                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass item,
                                  com.tngtech.archunit.lang.ConditionEvents events) {
                    item.getMethods().forEach(metodo ->
                            metodo.getParameters().forEach(parametro -> {
                                String declarado = null;

                                if (parametro.isAnnotatedWith(PathVariable.class)) {
                                    PathVariable a = parametro.getAnnotationOfType(PathVariable.class);
                                    declarado = !a.value().isEmpty() ? a.value() : a.name();
                                } else if (parametro.isAnnotatedWith(RequestParam.class)) {
                                    RequestParam a = parametro.getAnnotationOfType(RequestParam.class);
                                    declarado = !a.value().isEmpty() ? a.value() : a.name();
                                }

                                if (nomeProibido(declarado)) {
                                    events.add(com.tngtech.archunit.lang.SimpleConditionEvent
                                            .satisfied(item, metodo.getFullName()
                                                    + " aceita '" + declarado + "' do cliente"));
                                }
                            }));
                }
            };
        }

        static com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
        retornaClasseAnotadaComEntity() {
            return new com.tngtech.archunit.lang.ArchCondition<>("retornar @Entity") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass item,
                                  com.tngtech.archunit.lang.ConditionEvents events) {
                    item.getMethods().forEach(metodo -> {
                        var retorno = metodo.getRawReturnType();
                        if (retorno.isAnnotatedWith(Entity.class)) {
                            events.add(com.tngtech.archunit.lang.SimpleConditionEvent
                                    .satisfied(item, metodo.getFullName() + " retorna @Entity"));
                        }
                    });
                }
            };
        }
    }
}
