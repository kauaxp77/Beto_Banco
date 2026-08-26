package com.betobanco.shared.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sucessoSerializaComSuccessTrueEData() throws Exception {
        String json = mapper.writeValueAsString(ApiResponse.ok(new Exemplo("abc")));

        assertThat(json).isEqualTo("{\"success\":true,\"data\":{\"nome\":\"abc\"}}");
    }

    @Test
    void sucessoOmiteCampoErrorQuandoNulo() throws Exception {
        String json = mapper.writeValueAsString(ApiResponse.ok(new Exemplo("abc")));

        assertThat(json).doesNotContain("error");
    }

    @Test
    void listaPaginadaCarregaMetadadosCorretos() throws Exception {
        var page = new PageImpl<>(List.of(new Exemplo("a"), new Exemplo("b")),
                PageRequest.of(0, 20), 100);

        PageResponse<Exemplo> resposta = PageResponse.from(page);

        assertThat(resposta.success()).isTrue();
        assertThat(resposta.data()).hasSize(2);
        assertThat(resposta.pagination().page()).isEqualTo(0);
        assertThat(resposta.pagination().size()).isEqualTo(20);
        assertThat(resposta.pagination().totalElements()).isEqualTo(100);
        assertThat(resposta.pagination().totalPages()).isEqualTo(5);
    }

    record Exemplo(String nome) {}
}
