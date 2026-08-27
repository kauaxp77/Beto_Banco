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
import com.betobanco.support.TestAuth;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
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
class Fase4EndpointTest extends PostgresTestBase {

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

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    private String logar(String email, String senha) throws Exception {
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }

    private UUID criarCurso(String slug, UUID produtoId, int aulas) {
        Course curso = new Course("Curso " + slug, slug, null, null);
        curso.setPublished(true);
        UUID cursoId = courses.saveAndFlush(curso).getId();
        courseProducts.saveAndFlush(new CourseProduct(cursoId, produtoId));
        UUID moduloId = modules.saveAndFlush(new CourseModule(cursoId, "M1", 0)).getId();
        for (int i = 0; i < aulas; i++) {
            lessons.saveAndFlush(new Lesson(moduloId, "Aula " + (i + 1), null, null, 1800, i));
        }
        return cursoId;
    }

    @Test
    void certificadoSoSaiCom100PorCentoEValidaPublicamente() throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-F4-CERT", "Curso Certificavel", null, 49700L)).getId();
        UUID cursoId = criarCurso("f4-cert", produtoId, 2);

        usuarios.registrar("cert.f4@a.com", "senha-forte-123", "Formanda F4");
        var aluno = usuarios.buscarPorEmail("cert.f4@a.com").orElseThrow();
        entitlements.conceder(aluno.id(), produtoId, "MANUAL", "teste");
        String token = logar("cert.f4@a.com", "senha-forte-123");

        // Sem concluir tudo: 409 dizendo quantas faltam.
        mockMvc.perform(post("/courses/" + cursoId + "/certificate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // Conclui as 2 aulas.
        String detalhe = mockMvc.perform(get("/courses/" + cursoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> aulaIds = JsonPath.read(detalhe, "$.data.modules[0].lessons[*].id");
        for (String aulaId : aulaIds) {
            mockMvc.perform(post("/courses/lessons/" + aulaId + "/complete")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        // Emite; emitir de novo devolve o MESMO codigo (idempotente).
        String emitido = mockMvc.perform(post("/courses/" + cursoId + "/certificate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hours").value(1))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(emitido, "$.data.code");
        String deNovo = mockMvc.perform(post("/courses/" + cursoId + "/certificate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(deNovo, "$.data.code")).isEqualTo(codigo);

        // Validacao publica, SEM token.
        mockMvc.perform(get("/certificates/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentName").value("Formanda F4"))
                .andExpect(jsonPath("$.data.courseTitle").value("Curso f4-cert"));

        mockMvc.perform(get("/certificates/BB-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void comboViraTrilhaComProgressoAgregado() throws Exception {
        UUID comboId = produtos.saveAndFlush(
                new Product("SKU-F4-COMBO", "Combo F4 2 em 1", null, 99700L)).getId();
        UUID curso1 = criarCurso("f4-trilha-1", comboId, 2);
        criarCurso("f4-trilha-2", comboId, 3);

        usuarios.registrar("trilha.f4@a.com", "senha-forte-123", "Aluna Trilha");
        var aluno = usuarios.buscarPorEmail("trilha.f4@a.com").orElseThrow();
        entitlements.conceder(aluno.id(), comboId, "MANUAL", "teste");
        String token = logar("trilha.f4@a.com", "senha-forte-123");

        // Conclui 1 aula do curso 1.
        String detalhe = mockMvc.perform(get("/courses/" + curso1)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String aulaId = JsonPath.read(detalhe, "$.data.modules[0].lessons[0].id");
        mockMvc.perform(post("/courses/lessons/" + aulaId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String trilhas = mockMvc.perform(get("/courses/me/tracks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(trilhas, "$.data.length()")).isEqualTo(1);
        assertThat((int) JsonPath.read(trilhas, "$.data[0].totalLessons")).isEqualTo(5);
        assertThat((int) JsonPath.read(trilhas, "$.data[0].completedLessons")).isEqualTo(1);
        assertThat((int) JsonPath.read(trilhas, "$.data[0].courses.length()")).isEqualTo(2);

        // Streak: estudou hoje → streak atual 1.
        String stats = mockMvc.perform(get("/courses/me/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((boolean) JsonPath.read(stats, "$.data.studiedToday")).isTrue();
        assertThat((int) JsonPath.read(stats, "$.data.currentStreak")).isEqualTo(1);
    }

    @Test
    void depoimentoNascePendenteESoAparecePublicoDepoisDeAprovado() throws Exception {
        usuarios.registrar("depo.f4@a.com", "senha-forte-123", "Aluno Grato");
        String token = logar("depo.f4@a.com", "senha-forte-123");

        String criado = mockMvc.perform(post("/courses/testimonials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Passei no concurso graças ao método!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(criado, "$.data.id");

        // Publico ainda nao ve.
        String publicoAntes = mockMvc.perform(get("/testimonials"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) publicoAntes).doesNotContain("Passei no concurso");

        // Admin aprova → publico ve, sem e-mail do autor.
        String admin = TestAuth.logarComo(mockMvc, users, roles, "admin.f4@a.com", "ROLE_ADMIN");
        mockMvc.perform(post("/admin/courses/testimonials/" + id + "/status/APPROVED")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        String publicoDepois = mockMvc.perform(get("/testimonials"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) publicoDepois)
                .contains("Passei no concurso")
                .doesNotContain("depo.f4@a.com");
    }
}
