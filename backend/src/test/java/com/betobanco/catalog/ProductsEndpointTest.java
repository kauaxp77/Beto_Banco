package com.betobanco.catalog;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.support.PostgresTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vitrine publica do catalogo: sem autenticacao, apenas produtos ativos.
 * O banco e compartilhado entre classes de teste, entao as assercoes sao
 * "contem / nao contem", nunca sobre a lista inteira.
 */
@AutoConfigureMockMvc
class ProductsEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository produtos;

    @Test
    void listaPublicaSemAutenticacaoMostraApenasProdutosAtivos() throws Exception {
        produtos.saveAndFlush(new Product("SKU-PUB-A", "Curso Publico A", "descricao", 19900L));
        Product inativo = new Product("SKU-PUB-B", "Curso Desativado B", null, 9900L);
        inativo.setActive(false);
        produtos.saveAndFlush(inativo);

        String json = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        List<String> skus = JsonPath.read(json, "$.data[*].sku");
        assertThat(skus).contains("SKU-PUB-A").doesNotContain("SKU-PUB-B");

        List<Object> ativo = JsonPath.read(json, "$.data[?(@.sku=='SKU-PUB-A')]");
        assertThat(ativo).hasSize(1);
        assertThat((String) JsonPath.read(ativo.get(0), "$.name")).isEqualTo("Curso Publico A");
        assertThat((String) JsonPath.read(ativo.get(0), "$.description")).isEqualTo("descricao");
        assertThat(((Number) JsonPath.read(ativo.get(0), "$.priceCents")).longValue())
                .isEqualTo(19900L);
        assertThat((String) JsonPath.read(ativo.get(0), "$.currency")).isEqualTo("BRL");
    }
}
