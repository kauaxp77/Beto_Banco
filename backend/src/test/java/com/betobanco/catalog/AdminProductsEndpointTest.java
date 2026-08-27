package com.betobanco.catalog;

import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminProductsEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    private String logarCom(String email, String role) throws Exception {
        if (users.findByEmailIgnoreCase(email).isEmpty()) {
            Role r = roles.findByName(role).orElseThrow();
            User u = new User(email, "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"),
                    "Usuario Teste");
            u.getRoles().add(r);
            users.saveAndFlush(u);
        }
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }

    @Test
    void adminCriaAlteraEDesativaProduto() throws Exception {
        String token = logarCom("admin-prod@teste.com", "ROLE_ADMIN");

        // Criar.
        String criado = mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-ADM-1\",\"name\":\"Curso Admin\","
                                + "\"description\":\"desc\",\"priceCents\":29900}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("SKU-ADM-1"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(criado, "$.data.id");

        // Alterar preco e desativar.
        mockMvc.perform(put("/admin/products/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Curso Admin v2\",\"description\":\"nova\","
                                + "\"priceCents\":19900,\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Curso Admin v2"))
                .andExpect(jsonPath("$.data.priceCents").value(19900))
                .andExpect(jsonPath("$.data.active").value(false));

        // A lista do admin mostra o produto desativado; a vitrine publica nao.
        String listaAdmin = mockMvc.perform(get("/admin/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> skusAdmin = JsonPath.read(listaAdmin, "$.data[*].sku");
        assertThat(skusAdmin).contains("SKU-ADM-1");

        String listaPublica = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> skusPublicos = JsonPath.read(listaPublica, "$.data[*].sku");
        assertThat(skusPublicos).doesNotContain("SKU-ADM-1");
    }

    @Test
    void skuDuplicadoDevolve409() throws Exception {
        String token = logarCom("admin-prod@teste.com", "ROLE_ADMIN");
        String corpo = "{\"sku\":\"SKU-ADM-DUP\",\"name\":\"Original\","
                + "\"description\":null,\"priceCents\":1000}";

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void produtoInexistenteNoPutDevolve404() throws Exception {
        String token = logarCom("admin-prod@teste.com", "ROLE_ADMIN");

        mockMvc.perform(put("/admin/products/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"description\":null,"
                                + "\"priceCents\":1,\"active\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoNaoAcessaAdminProducts() throws Exception {
        String token = logarCom("aluno-prod@teste.com", "ROLE_STUDENT");

        mockMvc.perform(get("/admin/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
