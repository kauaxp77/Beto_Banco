package com.betobanco.users;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
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
class StudentEntitlementsEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository produtos;

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

    @Test
    void alunoListaOsProprosEntitlementsComDadosDoProduto() throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-ENT-ME", "Mentoria Completa", null, 49900L)).getId();
        usuarios.registrar("meusacessos@aluno.com", "senha-forte-123", "Aluna Acessos");
        var aluno = usuarios.buscarPorEmail("meusacessos@aluno.com").orElseThrow();
        entitlements.conceder(aluno.id(), produtoId, "MANUAL", "concessao-manual");

        String token = logar("meusacessos@aluno.com", "senha-forte-123");

        String json = mockMvc.perform(get("/students/me/entitlements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        List<Object> itens = JsonPath.read(json, "$.data[*]");
        assertThat(itens).hasSize(1);
        assertThat((String) JsonPath.read(itens.get(0), "$.productId"))
                .isEqualTo(produtoId.toString());
        assertThat((String) JsonPath.read(itens.get(0), "$.productName"))
                .isEqualTo("Mentoria Completa");
        assertThat((String) JsonPath.read(itens.get(0), "$.sku")).isEqualTo("SKU-ENT-ME");
        assertThat((String) JsonPath.read(itens.get(0), "$.source")).isEqualTo("MANUAL");
        assertThat((String) JsonPath.read(itens.get(0), "$.grantedAt")).isNotNull();
    }

    @Test
    void semTokenDevolve401NoEnvelopePadrao() throws Exception {
        mockMvc.perform(get("/students/me/entitlements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
