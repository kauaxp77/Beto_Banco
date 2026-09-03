package com.betobanco.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final EnvelopeAuthenticationEntryPoint entryPoint;
    private final EnvelopeAccessDeniedHandler accessDeniedHandler;
    private final String origensPermitidas;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          EnvelopeAuthenticationEntryPoint entryPoint,
                          EnvelopeAccessDeniedHandler accessDeniedHandler,
                          @Value("${betobanco.auth.jwt-secret}") String jwtSecret,
                          @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}") String origens) {
        // Falha rapido: subir sem segredo produziria tokens que qualquer um
        // consegue forjar, e o sintoma so apareceria em producao.
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou com menos de 32 bytes; HS256 exige pelo menos isso.");
        }
        this.jwtAuthFilter = jwtAuthFilter;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.origensPermitidas = origens;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ATENCAO: os matchers sao relativos ao context-path
                        // /api/v1, que o servlet remove antes de chegar aqui.
                        // Escrever "/api/v1/auth/**" nao casa com nada.
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products").permitAll()
                        // Validacao de certificado e prova social: publicos
                        // por natureza — recrutadores e a landing page.
                        .requestMatchers(HttpMethod.GET, "/certificates/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/testimonials").permitAll()
                        // Secao 22 -- os textos legais precisam ser lidos antes do
                        // aceite, e o aceite acontece no checkout, antes de existir
                        // conta. Termo de uso atras de login nao informa ninguem.
                        .requestMatchers(HttpMethod.GET, "/legal/*").permitAll()
                        // Secoes 07/11/15 -- a ficha de concurso e "pagina completa
                        // e indexavel" e e a fonte de trafego organico do plano.
                        // Atras de login ela nao e indexavel por ninguem.
                        .requestMatchers(HttpMethod.GET, "/contests/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                            .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(
                Arrays.stream(origensPermitidas.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("X-Trace-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
