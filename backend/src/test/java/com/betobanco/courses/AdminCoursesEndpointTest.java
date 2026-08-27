package com.betobanco.courses;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.support.TestAuth;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminCoursesEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private ProductRepository produtos;

    private String tokenAdmin() throws Exception {
        return TestAuth.logarComo(mockMvc, users, roles, "admin.cursos@a.com", "ROLE_ADMIN");
    }

    @Test
    void adminMontaCursoCompletoComModuloAulaEProduto() throws Exception {
        String token = tokenAdmin();
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-ADM-CURSO", "Produto do Curso", null, 99700L)).getId();

        // Criar curso: slug derivado do titulo, sem acentos.
        String criado = mockMvc.perform(post("/admin/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mentoria Aprovação BB\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("mentoria-aprovacao-bb"))
                .andExpect(jsonPath("$.data.published").value(false))
                .andReturn().getResponse().getContentAsString();
        String cursoId = JsonPath.read(criado, "$.data.id");

        // Vincular produto.
        mockMvc.perform(post("/admin/courses/" + cursoId + "/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + produtoId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productIds[0]").value(produtoId.toString()));

        // Criar modulo e aula.
        mockMvc.perform(post("/admin/courses/" + cursoId + "/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Módulo 1\",\"position\":0}"))
                .andExpect(status().isCreated());

        String detalhe = mockMvc.perform(get("/admin/courses/" + cursoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((int) JsonPath.read(detalhe, "$.data.moduleCount")).isEqualTo(1);

        // Publicar o curso.
        mockMvc.perform(put("/admin/courses/" + cursoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mentoria Aprovação BB\",\"published\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(true));
    }

    @Test
    void tituloRepetidoDevolve409() throws Exception {
        String token = tokenAdmin();
        mockMvc.perform(post("/admin/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Curso Repetido\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/admin/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Curso Repetido\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void alunoComumNaoAcessaGestaoDeCursos() throws Exception {
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "aluno.gestao@a.com", "ROLE_STUDENT");
        mockMvc.perform(get("/admin/courses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void removerAulaInexistenteDevolve404() throws Exception {
        String token = tokenAdmin();
        mockMvc.perform(delete("/admin/courses/lessons/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
