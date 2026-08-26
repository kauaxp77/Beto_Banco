package com.betobanco.shared.exception;

import com.betobanco.support.PostgresTestBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.betobanco.shared.trace.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest extends PostgresTestBase {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void excecaoDeNegocioViraEnvelopeDeErro() throws Exception {
        mockMvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.path").value("/teste/nao-encontrado"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void validacaoRetorna422ComListaDeCampos() throws Exception {
        mockMvc.perform(post("/teste/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("nome"));
    }

    @Test
    void corpoIlegivelRetorna400() throws Exception {
        mockMvc.perform(post("/teste/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void excecaoInesperadaNaoVazaDetalheInterno() throws Exception {
        mockMvc.perform(get("/teste/explode"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Erro interno do servidor"));
    }

    @TestConfiguration
    static class ControllerDeTeste {
        @Bean
        ControllerFalso controllerFalso() {
            return new ControllerFalso();
        }
    }

    @RestController
    @RequestMapping("/teste")
    static class ControllerFalso {

        @org.springframework.web.bind.annotation.GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new NotFoundException("Recurso não encontrado");
        }

        @org.springframework.web.bind.annotation.GetMapping("/explode")
        void explode() {
            throw new IllegalStateException("detalhe interno que nao pode vazar");
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Entrada entrada) {
        }

        record Entrada(@NotBlank String nome) {}
    }
}
