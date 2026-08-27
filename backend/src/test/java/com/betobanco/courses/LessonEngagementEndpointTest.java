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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LessonEngagementEndpointTest extends PostgresTestBase {

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

    private record Cenario(UUID produtoId, UUID cursoId, UUID aulaId, String tokenAluno) {
    }

    private Cenario montar(String sufixo) throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-ENG-" + sufixo, "Curso " + sufixo, null, 49700L)).getId();
        Course curso = new Course("Engajamento " + sufixo,
                "engajamento-" + sufixo.toLowerCase(), null, null);
        curso.setPublished(true);
        UUID cursoId = courses.saveAndFlush(curso).getId();
        courseProducts.saveAndFlush(new CourseProduct(cursoId, produtoId));
        UUID moduloId = modules.saveAndFlush(new CourseModule(cursoId, "M1", 0)).getId();
        UUID aulaId = lessons.saveAndFlush(new Lesson(moduloId, "Aula", null, null, null, 0))
                .getId();

        String email = "aluno.eng" + sufixo.toLowerCase() + "@a.com";
        usuarios.registrar(email, "senha-forte-123", "Aluno Engajado");
        var aluno = usuarios.buscarPorEmail(email).orElseThrow();
        entitlements.conceder(aluno.id(), produtoId, "MANUAL", "teste");
        return new Cenario(produtoId, cursoId, aulaId, logar(email, "senha-forte-123"));
    }

    @Test
    void alunoComentaAvaliaEAdminModera() throws Exception {
        Cenario c = montar("A");

        // Comentar.
        String criado = mockMvc.perform(post("/courses/lessons/" + c.aulaId() + "/comments")
                        .header("Authorization", "Bearer " + c.tokenAluno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Professor, essa aula vale para a CESGRANRIO?\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String comentarioId = JsonPath.read(criado, "$.data.id");

        // Avaliar como util (duas vezes: upsert, nao duplica).
        mockMvc.perform(put("/courses/lessons/" + c.aulaId() + "/rating")
                        .header("Authorization", "Bearer " + c.tokenAluno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpful\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/courses/lessons/" + c.aulaId() + "/rating")
                        .header("Authorization", "Bearer " + c.tokenAluno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpful\":true}"))
                .andExpect(status().isOk());

        // Discussao: 1 comentario visivel, 1 voto util, meu voto = true.
        String discussao = mockMvc.perform(
                        get("/courses/lessons/" + c.aulaId() + "/discussion")
                                .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(discussao, "$.data.comments.length()")).isEqualTo(1);
        assertThat((boolean) JsonPath.read(discussao, "$.data.comments[0].mine")).isTrue();
        assertThat((int) JsonPath.read(discussao, "$.data.helpfulCount")).isEqualTo(1);
        assertThat((boolean) JsonPath.read(discussao, "$.data.myRating")).isTrue();

        // Admin responde e depois oculta o comentario original.
        String tokenAdmin = TestAuth.logarComo(mockMvc, users, roles,
                "admin.eng@a.com", "ROLE_ADMIN");
        mockMvc.perform(post("/admin/courses/comments/" + comentarioId + "/reply")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Vale sim! Foco total.\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/admin/courses/comments/" + comentarioId + "/hide")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        // Para o aluno sobra so a resposta do professor, marcada como instrutor.
        String depois = mockMvc.perform(
                        get("/courses/lessons/" + c.aulaId() + "/discussion")
                                .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(depois, "$.data.comments.length()")).isEqualTo(1);
        assertThat((boolean) JsonPath.read(depois, "$.data.comments[0].instructor")).isTrue();
    }

    @Test
    void materiaisAparecemNoDetalheDoCurso() throws Exception {
        Cenario c = montar("B");
        String tokenAdmin = TestAuth.logarComo(mockMvc, users, roles,
                "admin.eng@a.com", "ROLE_ADMIN");

        mockMvc.perform(post("/admin/courses/lessons/" + c.aulaId() + "/materials")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Apostila em PDF\",\"url\":\"https://cdn.x/apostila.pdf\",\"position\":0}"))
                .andExpect(status().isCreated());

        String detalhe = mockMvc.perform(get("/courses/" + c.cursoId())
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(detalhe,
                "$.data.modules[0].lessons[0].materials[0].title")).isEqualTo("Apostila em PDF");
    }

    @Test
    void aulaSemAcessoNaoAceitaComentarioNemVoto() throws Exception {
        Cenario c = montar("C");
        usuarios.registrar("intruso.eng@a.com", "senha-forte-123", "Sem Acesso");
        String tokenIntruso = logar("intruso.eng@a.com", "senha-forte-123");

        mockMvc.perform(post("/courses/lessons/" + c.aulaId() + "/comments")
                        .header("Authorization", "Bearer " + tokenIntruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"oi\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/courses/lessons/" + c.aulaId() + "/discussion")
                        .header("Authorization", "Bearer " + tokenIntruso))
                .andExpect(status().isNotFound());
    }
}
