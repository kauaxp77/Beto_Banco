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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class QuizEndpointTest extends PostgresTestBase {

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

    private record Cenario(UUID cursoId, UUID aulaId, String tokenAluno, String tokenAdmin) {
    }

    private Cenario montar(String sufixo) throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-QZ-" + sufixo, "Curso Quiz " + sufixo, null, 49700L)).getId();
        Course curso = new Course("Quiz " + sufixo, "quiz-" + sufixo.toLowerCase(), null, null);
        curso.setPublished(true);
        UUID cursoId = courses.saveAndFlush(curso).getId();
        courseProducts.saveAndFlush(new CourseProduct(cursoId, produtoId));
        UUID moduloId = modules.saveAndFlush(new CourseModule(cursoId, "M1", 0)).getId();
        UUID aulaId = lessons.saveAndFlush(
                new Lesson(moduloId, "Bateria de questões", null, null, null, 0)).getId();

        String email = "aluno.qz" + sufixo.toLowerCase() + "@a.com";
        usuarios.registrar(email, "senha-forte-123", "Aluno Quiz");
        var aluno = usuarios.buscarPorEmail(email).orElseThrow();
        entitlements.conceder(aluno.id(), produtoId, "MANUAL", "teste");

        String tokenAdmin = TestAuth.logarComo(mockMvc, users, roles,
                "admin.qz@a.com", "ROLE_ADMIN");
        return new Cenario(cursoId, aulaId, logar(email, "senha-forte-123"), tokenAdmin);
    }

    private String criarQuestao(Cenario c, String enunciado, int correta) throws Exception {
        String json = mockMvc.perform(post("/admin/courses/lessons/" + c.aulaId() + "/questions")
                        .header("Authorization", "Bearer " + c.tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statement\":\"" + enunciado + "\","
                                + "\"options\":[\"Alternativa A\",\"Alternativa B\","
                                + "\"Alternativa C\"],"
                                + "\"correctIndex\":" + correta + ",\"position\":0,"
                                + "\"explanation\":\"Comentário do professor.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.id");
    }

    @Test
    void alunoNaoVeGabaritoAntesEntregaCorrigidaMarcaProgresso() throws Exception {
        Cenario c = montar("A");
        String q1 = criarQuestao(c, "Quanto é 2 + 2?", 1);
        String q2 = criarQuestao(c, "Capital do Brasil?", 2);

        // Antes da entrega: enunciados sem correctIndex nem explanation.
        String quiz = mockMvc.perform(get("/courses/lessons/" + c.aulaId() + "/quiz")
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(quiz, "$.data.questions.length()")).isEqualTo(2);
        assertThat((String) quiz).doesNotContain("correctIndex").doesNotContain("Comentário");

        // Entrega: acerta a 1, erra a 2 → 50%, gabarito comentado na resposta.
        String resultado = mockMvc.perform(
                        post("/courses/lessons/" + c.aulaId() + "/quiz/submit")
                                .header("Authorization", "Bearer " + c.tokenAluno())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"answers\":["
                                        + "{\"questionId\":\"" + q1 + "\",\"answerIndex\":1},"
                                        + "{\"questionId\":\"" + q2 + "\",\"answerIndex\":0}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.scorePct").value(50))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) resultado).contains("Comentário do professor.");

        // Entregar contou como aula concluida e a tentativa ficou no historico.
        String detalhe = mockMvc.perform(get("/courses/" + c.cursoId())
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andReturn().getResponse().getContentAsString();
        assertThat((boolean) JsonPath.read(detalhe,
                "$.data.modules[0].lessons[0].completed")).isTrue();
        assertThat((int) JsonPath.read(detalhe,
                "$.data.modules[0].lessons[0].questionCount")).isEqualTo(2);

        String depois = mockMvc.perform(get("/courses/lessons/" + c.aulaId() + "/quiz")
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(depois, "$.data.myAttempts.length()")).isEqualTo(1);
    }

    @Test
    void entregaIncompletaEIntrusoSaoRecusados() throws Exception {
        Cenario c = montar("B");
        String q1 = criarQuestao(c, "Questão única?", 0);
        criarQuestao(c, "Segunda questão?", 0);

        // Faltou responder a segunda: 422.
        mockMvc.perform(post("/courses/lessons/" + c.aulaId() + "/quiz/submit")
                        .header("Authorization", "Bearer " + c.tokenAluno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":\"" + q1
                                + "\",\"answerIndex\":0}]}"))
                .andExpect(status().isUnprocessableEntity());

        // Sem entitlement: a aula nao existe.
        usuarios.registrar("intruso.qz@a.com", "senha-forte-123", "Sem Acesso");
        String intruso = logar("intruso.qz@a.com", "senha-forte-123");
        mockMvc.perform(get("/courses/lessons/" + c.aulaId() + "/quiz")
                        .header("Authorization", "Bearer " + intruso))
                .andExpect(status().isNotFound());
    }
}
