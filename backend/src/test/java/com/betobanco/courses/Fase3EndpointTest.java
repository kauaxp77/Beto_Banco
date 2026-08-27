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
class Fase3EndpointTest extends PostgresTestBase {

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

    private record Cenario(UUID produtoId, UUID cursoId, String tokenAluno) {
    }

    private Cenario montar(String sufixo) throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-F3-" + sufixo, "Curso F3 " + sufixo, null, 49700L)).getId();
        Course curso = new Course("Fase Tres " + sufixo, "fase3-" + sufixo.toLowerCase(),
                null, null);
        curso.setPublished(true);
        UUID cursoId = courses.saveAndFlush(curso).getId();
        courseProducts.saveAndFlush(new CourseProduct(cursoId, produtoId));
        UUID moduloId = modules.saveAndFlush(new CourseModule(cursoId, "M1", 0)).getId();
        lessons.saveAndFlush(new Lesson(moduloId, "Aula 1", null, null, null, 0));

        String email = "aluno.f3" + sufixo.toLowerCase() + "@a.com";
        usuarios.registrar(email, "senha-forte-123", "Aluno F3");
        var aluno = usuarios.buscarPorEmail(email).orElseThrow();
        entitlements.conceder(aluno.id(), produtoId, "MANUAL", "teste");
        return new Cenario(produtoId, cursoId, logar(email, "senha-forte-123"));
    }

    @Test
    void anuncioDeCursoChegaSoParaQuemTemAcesso() throws Exception {
        Cenario c = montar("A");
        String admin = TestAuth.logarComo(mockMvc, users, roles, "admin.f3@a.com", "ROLE_ADMIN");

        // Anuncio geral + anuncio do curso.
        mockMvc.perform(post("/admin/courses/announcements")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aviso geral\",\"body\":\"Plataforma atualizada.\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/admin/courses/announcements")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + c.cursoId()
                                + "\",\"title\":\"Aula extra sábado\",\"body\":\"Ao vivo 10h.\"}"))
                .andExpect(status().isCreated());

        // Quem comprou ve os dois.
        String doAluno = mockMvc.perform(get("/courses/announcements")
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) doAluno).contains("Aviso geral").contains("Aula extra sábado");

        // Quem nao comprou ve apenas o geral.
        usuarios.registrar("sem.f3@a.com", "senha-forte-123", "Sem Curso");
        String tokenSem = logar("sem.f3@a.com", "senha-forte-123");
        String doOutro = mockMvc.perform(get("/courses/announcements")
                        .header("Authorization", "Bearer " + tokenSem))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) doOutro).contains("Aviso geral").doesNotContain("Aula extra sábado");
    }

    @Test
    void conviteCriaContaConcedeAcessoComValidadeEListaDepois() throws Exception {
        Cenario c = montar("B");
        String admin = TestAuth.logarComo(mockMvc, users, roles, "admin.f3@a.com", "ROLE_ADMIN");

        String criado = mockMvc.perform(post("/admin/invites")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bolsista.f3@a.com\",\"fullName\":\"Bolsista F3\","
                                + "\"productId\":\"" + c.produtoId() + "\",\"validadeDias\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contaNova").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(criado, "$.data.expiresAt")).isNotNull();

        // A conta existe sem senha e o acesso esta vigente.
        var convidado = usuarios.buscarPorEmail("bolsista.f3@a.com").orElseThrow();
        assertThat(entitlements.temAcesso(convidado.id(), c.produtoId())).isTrue();

        // Convite aparece na listagem.
        String lista = mockMvc.perform(get("/admin/invites")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) lista).contains("bolsista.f3@a.com");
    }

    @Test
    void relatoriosTrazemAlunosEConclusoes() throws Exception {
        Cenario c = montar("C");
        String admin = TestAuth.logarComo(mockMvc, users, roles, "admin.f3@a.com", "ROLE_ADMIN");

        String geral = mockMvc.perform(get("/admin/courses/reports")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) geral).contains("Fase Tres C");

        String porAula = mockMvc.perform(get("/admin/courses/" + c.cursoId() + "/reports")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(porAula, "$.data.students")).isEqualTo(1);
        assertThat((int) JsonPath.read(porAula, "$.data.lessons.length()")).isEqualTo(1);
    }

    @Test
    void endpointsDeGestaoExigemAdmin() throws Exception {
        Cenario c = montar("D");
        mockMvc.perform(get("/admin/courses/reports")
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/invites")
                        .header("Authorization", "Bearer " + c.tokenAluno()))
                .andExpect(status().isForbidden());
    }
}
