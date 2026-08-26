package com.betobanco.shared.trace;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void geraTraceIdQuandoClienteNaoEnvia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isNotBlank();
    }

    @Test
    void propagaTraceIdEnviadoPeloCliente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, "trace-do-cliente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("trace-do-cliente");
    }

    @Test
    void descartaTraceIdComCaractereInvalido() throws Exception {
        String forjado = "abc123 def\nlevel=ERROR usuario=admin";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, forjado);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        String[] capturado = new String[1];
        doAnswer(invocation -> {
            capturado[0] = MDC.get(TraceIdFilter.MDC_KEY);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER))
                .isNotBlank()
                .isNotEqualTo(forjado)
                .matches("[A-Za-z0-9._-]{1,64}");
        assertThat(capturado[0]).isEqualTo(response.getHeader(TraceIdFilter.HEADER));
    }

    @Test
    void descartaTraceIdLongoDemais() throws Exception {
        String longoDemais = "a".repeat(65);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, longoDemais);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        String[] capturado = new String[1];
        doAnswer(invocation -> {
            capturado[0] = MDC.get(TraceIdFilter.MDC_KEY);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER))
                .isNotBlank()
                .isNotEqualTo(longoDemais)
                .matches("[A-Za-z0-9._-]{1,64}");
        assertThat(capturado[0]).isEqualTo(response.getHeader(TraceIdFilter.HEADER));
    }

    @Test
    void disponibilizaTraceIdNoMdcDuranteAChamadaELimpaDepois() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        String[] capturado = new String[1];
        doAnswer(invocation -> {
            capturado[0] = MDC.get(TraceIdFilter.MDC_KEY);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(capturado[0]).isNotBlank();
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
