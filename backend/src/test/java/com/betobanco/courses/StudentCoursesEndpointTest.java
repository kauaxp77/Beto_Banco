package com.betobanco.courses;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseModule;
import com.betobanco.courses.entity.CourseProduct;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.CourseProductRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StudentCoursesEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private CourseModuleRepository modules;

    @Autowired
    private LessonRepository lessons;

    @Autowired
    private CourseProductRepository courseProducts;

    @Autowired
    private UserDirectory usuarios;

    @Autowired
    private EntitlementService entitlements;

    private String logar(String email, String senha) throws Exception {
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }

    /** Curso publicado, com 1 modulo e 2 aulas, vendido pelo produto criado. */
    private Cenario montarCenario(String sufixo) {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-CURSO-" + sufixo, "Curso " + sufixo, null, 49700L)).getId();

        Course curso = new Course("Mentoria " + sufixo, "mentoria-" + sufixo.toLowerCase(),
                "Preparatorio completo", null);
        curso.setPublished(true);
        UUID cursoId = courses.saveAndFlush(curso).getId();
        courseProducts.saveAndFlush(new CourseProduct(cursoId, produtoId));

        UUID moduloId = modules.saveAndFlush(
                new CourseModule(cursoId, "Modulo 1", 0)).getId();
        UUID aula1 = lessons.saveAndFlush(new Lesson(moduloId, "Aula 1", null,
                "https://www.youtube.com/watch?v=abc", 600, 0)).getId();
        UUID aula2 = lessons.saveAndFlush(new Lesson(moduloId, "Aula 2", null,
                "https://www.youtube.com/watch?v=def", 900, 1)).getId();

        return new Cenario(produtoId, cursoId, aula1, aula2);
    }

    private record Cenario(UUID produtoId, UUID cursoId, UUID aula1, UUID aula2) {
    }

    @Test
    void alunoComEntitlementVeCursoComProgresso() throws Exception {
        Cenario c = montarCenario("A");
        usuarios.registrar("aluno.curso@a.com", "senha-forte-123", "Aluno Curso");
        var aluno = usuarios.buscarPorEmail("aluno.curso@a.com").orElseThrow();
        entitlements.conceder(aluno.id(), c.produtoId(), "MANUAL", "teste");

        String token = logar("aluno.curso@a.com", "senha-forte-123");

        // Home: 1 curso, 2 aulas, 0 concluidas, proxima = aula 1.
        String json = mockMvc.perform(get("/courses/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        List<Object> cursosDoAluno = JsonPath.read(json, "$.data[*]");
        assertThat(cursosDoAluno).hasSize(1);
        assertThat((int) JsonPath.read(json, "$.data[0].totalLessons")).isEqualTo(2);
        assertThat((int) JsonPath.read(json, "$.data[0].completedLessons")).isEqualTo(0);
        assertThat((String) JsonPath.read(json, "$.data[0].nextLessonId"))
                .isEqualTo(c.aula1().toString());

        // Concluir a aula 1 (duas vezes: idempotente).
        mockMvc.perform(post("/courses/lessons/" + c.aula1() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/courses/lessons/" + c.aula1() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String depois = mockMvc.perform(get("/courses/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(depois, "$.data[0].completedLessons")).isEqualTo(1);
        assertThat((String) JsonPath.read(depois, "$.data[0].nextLessonId"))
                .isEqualTo(c.aula2().toString());

        // Detalhe: modulo com as 2 aulas, primeira marcada como concluida.
        String detalhe = mockMvc.perform(get("/courses/" + c.cursoId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(detalhe, "$.data.modules.length()")).isEqualTo(1);
        assertThat((boolean) JsonPath.read(detalhe, "$.data.modules[0].lessons[0].completed"))
                .isTrue();
        assertThat((boolean) JsonPath.read(detalhe, "$.data.modules[0].lessons[1].completed"))
                .isFalse();
    }

    @Test
    void cursoSemEntitlementNaoExisteParaOAluno() throws Exception {
        Cenario c = montarCenario("B");
        usuarios.registrar("aluno.sem@a.com", "senha-forte-123", "Aluno Sem Acesso");
        String token = logar("aluno.sem@a.com", "senha-forte-123");

        mockMvc.perform(get("/courses/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 404 identico ao de curso inexistente: nao vaza catalogo.
        mockMvc.perform(get("/courses/" + c.cursoId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/courses/lessons/" + c.aula1() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cursoNaoPublicadoFicaInvisivelMesmoComEntitlement() throws Exception {
        Cenario c = montarCenario("C");
        Course curso = courses.findById(c.cursoId()).orElseThrow();
        curso.setPublished(false);
        courses.saveAndFlush(curso);

        usuarios.registrar("aluno.rascunho@a.com", "senha-forte-123", "Aluno Rascunho");
        var aluno = usuarios.buscarPorEmail("aluno.rascunho@a.com").orElseThrow();
        entitlements.conceder(aluno.id(), c.produtoId(), "MANUAL", "teste");
        String token = logar("aluno.rascunho@a.com", "senha-forte-123");

        mockMvc.perform(get("/courses/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/courses/" + c.cursoId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void semTokenDevolve401NoEnvelopePadrao() throws Exception {
        mockMvc.perform(get("/courses/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
