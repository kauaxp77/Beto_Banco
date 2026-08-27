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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void rotaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/teste/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.path").value("/teste/rota-que-nao-existe"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty());
    }

    @Test
    void metodoHttpNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(delete("/teste/validar"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.error.status").value(405))
                .andExpect(jsonPath("$.error.path").value("/teste/validar"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty());
    }

    @Test
    void requestParamObrigatorioAusenteRetorna400() throws Exception {
        mockMvc.perform(get("/teste/busca"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.path").value("/teste/busca"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty());
    }

    @Test
    void pathVariableDeTipoErradoRetorna400() throws Exception {
        mockMvc.perform(get("/teste/num/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.path").value("/teste/num/abc"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty());
    }

    @Test
    void responseStatusExceptionPreservaOStatusDeclarado() throws Exception {
        mockMvc.perform(get("/teste/status-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.path").value("/teste/status-404"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty());
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

        @org.springframework.web.bind.annotation.GetMapping("/busca")
        void busca(@RequestParam String termo) {
        }

        @org.springframework.web.bind.annotation.GetMapping("/num/{numero}")
        void numero(@PathVariable Long numero) {
        }

        @org.springframework.web.bind.annotation.GetMapping("/status-404")
        void statusExplicito() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "recurso ausente");
        }

        record Entrada(@NotBlank String nome) {}
    }
}
