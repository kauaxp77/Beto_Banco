package br.com.aprovacao.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Secao 20 (perfis e permissoes) e secao 21 (seguranca).
 *
 * <p>A tabela de permissao da secao 20 aparece aqui em dois niveis: os matchers
 * abaixo dao o corte grosso por area, e {@code @PreAuthorize} nos servicos faz o
 * corte fino -- "professor edita as proprias aulas" nao cabe em um matcher de URL.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracaoSeguranca {

    private final PropriedadesPlataforma props;

    public ConfiguracaoSeguranca(PropriedadesPlataforma props) {
        this.props = props;
    }

    /** Secao 21: BCrypt custo 12. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, FiltroJwt filtroJwt) throws Exception {
        http
            // API sem cookie de sessao: o CSRF classico nao se aplica, e o token vai
            // no cabecalho Authorization, que um formulario de terceiro nao consegue
            // preencher. Cookie de refresh, se um dia existir, exige rever isto.
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Secao 21 -- cabecalhos.
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .xssProtection(x -> x.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        // A API so devolve JSON; a unica pagina HTML servida e o Swagger.
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")))

            .authorizeHttpRequests(reg -> reg
                // Publico
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/docs", "/api/docs/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/webhooks/**").permitAll()   // autenticado por HMAC, secao 12
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/concursos/**",
                        "/api/v1/cursos/**",
                        "/api/v1/carreiras/**",
                        "/api/v1/posts/**",
                        "/api/v1/busca",
                        "/api/v1/legal/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/leads", "/api/v1/pedidos").permitAll()

                // Secao 20 -- corte por perfil
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/suporte/**").hasAnyRole("SUPORTE", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/correcoes/**").hasAnyRole("CORRETOR", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/professor/**").hasAnyRole("PROFESSOR", "ADMIN", "SUPER_ADMIN")

                .anyRequest().authenticated())

            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Lista explicita. Curinga com credenciais e recusado pelo navegador e, pior,
        // abriria a API para qualquer origem se um dia as credenciais saissem.
        cfg.setAllowedOrigins(props.seguranca().corsOrigens());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Trace-Id"));
        cfg.setExposedHeaders(List.of("X-Trace-Id", "Retry-After"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cfg);
        return fonte;
    }
}
